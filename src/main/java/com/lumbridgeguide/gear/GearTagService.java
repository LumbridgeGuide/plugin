package com.lumbridgeguide.gear;

import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.gear.data.PluginGearData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.banktags.tabs.TabManager;
import net.runelite.client.plugins.banktags.tabs.TagTab;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GearTagService {

    private static final Pattern GEAR_CODE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String FALLBACK_TAG = "gear";

    private final LumbridgeGuideClient apiClient;
    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final TagManager tagManager;
    private final LayoutManager layoutManager;
    private final TabManager tabManager;
    private final BankTagsService bankTagsService;

    public static Optional<String> extractCode(String codeOrLink) {
        if (codeOrLink == null) {
            return Optional.empty();
        }
        Matcher matcher = GEAR_CODE.matcher(codeOrLink.trim());
        return matcher.find() ? Optional.of(matcher.group().toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    private static String tagName(String gearName) {
        String slug = gearName == null ? "" : gearName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return "lg-" + (slug.isEmpty() ? FALLBACK_TAG : slug);
    }

    public void generate(String code, boolean includeMissing, Consumer<GearResult> onComplete) {
        apiClient.get("/plugin/gear/" + code,
                response -> build(response, includeMissing, onComplete),
                response -> onComplete.accept(GearResult.failure(failureMessage(response))));
    }

    private void build(ApiResponse response, boolean includeMissing, Consumer<GearResult> onComplete) {
        PluginGearData gear = apiClient.deserialize(response.getBody(), PluginGearData.class);
        if (gear == null || gear.getItemIds() == null || gear.getItemIds().isEmpty()) {
            onComplete.accept(GearResult.failure("That setup has no items"));
            return;
        }

        clientThread.invoke(() -> onComplete.accept(applyToBank(gear, includeMissing)));
    }

    private GearResult applyToBank(PluginGearData gear, boolean includeMissing) {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) {
            return GearResult.failure("Open your bank first");
        }

        Set<Integer> owned = new HashSet<>();
        Map<Integer, Integer> ownedByVariation = new HashMap<>();
        readBank(bank, owned, ownedByVariation);

        int[] shape = GearBankLayout.positions(gear);
        int[] placed = new int[shape.length];
        Set<Integer> tagged = new LinkedHashSet<>();
        Set<Integer> missing = new LinkedHashSet<>();
        for (int position = 0; position < shape.length; position++) {
            int itemId = shape[position];
            placed[position] = -1;
            if (itemId < 0) {
                continue;
            }
            int bankItem = bankItemFor(itemId, owned, ownedByVariation, ItemVariationMapping::map);
            if (bankItem < 0) {
                missing.add(itemId);
            }
            if (bankItem >= 0 || includeMissing) {
                int shown = bankItem >= 0 ? bankItem : itemId;
                placed[position] = shown;
                tagged.add(shown);
            }
        }

        String tag = tagName(gear.getName());
        writeTag(tag, gear, tagged, placed);
        log.debug("Gear tag '{}' generated: {} item(s), {} missing", tag, tagged.size(), missing.size());
        String summary = summary(tag, tagged.size(), missing.size(), includeMissing);
        return new GearResult(true, summary, null, itemNames(missing));
    }

    private void readBank(ItemContainer bank, Set<Integer> owned, Map<Integer, Integer> ownedByVariation) {
        for (Item item : bank.getItems()) {
            if (item.getId() > 0 && item.getQuantity() > 0) {
                int itemId = itemManager.canonicalize(item.getId());
                owned.add(itemId);
                ownedByVariation.putIfAbsent(ItemVariationMapping.map(itemId), itemId);
            }
        }
    }

    private void writeTag(String tag, PluginGearData gear, Set<Integer> tagged, int[] placed) {
        tagManager.removeTag(tag);
        for (int itemId : tagged) {
            tagManager.addTag(itemId, tag, false);
        }
        layoutManager.saveLayout(new Layout(tag, placed));
        if (tabManager.find(tag) == null && !tagged.isEmpty()) {
            TagTab tab = new TagTab();
            tab.setTag(tag);
            tab.setIconItemId(iconItem(gear, tagged));
            tabManager.add(tab);
            tabManager.save();
        }
        bankTagsService.openBankTag(tag, BankTagsService.OPTION_ALLOW_MODIFICATIONS);
    }

    private List<String> itemNames(Set<Integer> itemIds) {
        List<String> names = new ArrayList<>();
        for (int itemId : itemIds) {
            names.add(itemManager.getItemComposition(itemId).getName());
        }
        return names;
    }

    private static int bankItemFor(int itemId, Set<Integer> owned, Map<Integer, Integer> ownedByVariation,
                           IntUnaryOperator variation) {
        if (owned.contains(itemId)) {
            return itemId;
        }
        return ownedByVariation.getOrDefault(variation.applyAsInt(itemId), -1);
    }

    private static int iconItem(PluginGearData gear, Set<Integer> tagged) {
        Integer weapon = gear.getEquipment() == null ? null : gear.getEquipment().get("weapon");
        if (weapon != null && tagged.contains(weapon)) {
            return weapon;
        }
        return tagged.iterator().next();
    }

    private static String summary(String tag, int tagged, int missing, boolean includeMissing) {
        String base = "Tagged " + tagged + " item" + (tagged == 1 ? "" : "s") + " as \"" + tag + "\"";
        if (missing == 0) {
            return base + ".";
        }
        String items = missing + " item" + (missing == 1 ? "" : "s");
        return includeMissing
                ? base + ". " + items + " not in your bank, shown as placeholders."
                : base + ". " + items + " not in your bank " + (missing == 1 ? "was" : "were") + " left out.";
    }

    private String failureMessage(ApiResponse response) {
        return response.getStatusCode() == 404
                ? "No setup found for that code"
                : apiClient.failureMessage(response, "Couldn't make the tab");
    }
}
