package com.lumbridgeguide.gear;

import com.google.gson.Gson;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.gear.data.OwnedGearConfig;
import com.lumbridgeguide.gear.data.OwnedGearConfigPage;
import com.lumbridgeguide.gear.data.PluginGearData;
import lombok.Value;
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

/**
 * Builds a bank tag tab from a Lumbridge Guide gear config while the bank is open. Items the player has are tagged
 * with the config's name and laid out in the config's shape ({@link GearBankLayout}). Missing items can be included
 * too: RuneLite draws a layout item that is not in the bank as a faded placeholder. Generating the same config again
 * replaces the tag and its layout.
 */
@Slf4j
@Singleton
public class GearTagService {

    private static final Pattern GEAR_CODE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String FALLBACK_TAG = "gear";

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;
    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final TagManager tagManager;
    private final LayoutManager layoutManager;
    private final TabManager tabManager;
    private final BankTagsService bankTagsService;

    @Inject
    public GearTagService(
            LumbridgeGuideClient apiClient,
            Gson gson,
            Client client,
            ClientThread clientThread,
            ItemManager itemManager,
            TagManager tagManager,
            LayoutManager layoutManager,
            TabManager tabManager,
            BankTagsService bankTagsService) {
        this.apiClient = apiClient;
        this.gson = gson;
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        this.tagManager = tagManager;
        this.layoutManager = layoutManager;
        this.tabManager = tabManager;
        this.bankTagsService = bankTagsService;
    }

    @Value
    public static class Result {
        boolean success;
        String message;
        /** Names of the config's items that were not in the bank, in layout order. */
        List<String> missingItems;

        static Result failure(String message) {
            return new Result(false, message, List.of());
        }
    }

    /**
     * Accepts either a bare gear code or a full gear page link.
     */
    public static Optional<String> extractCode(String input) {
        if (input == null) {
            return Optional.empty();
        }
        Matcher matcher = GEAR_CODE.matcher(input.trim());
        return matcher.find() ? Optional.of(matcher.group().toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    public static String tagName(String gearName) {
        String slug = gearName == null ? "" : gearName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? FALLBACK_TAG : slug;
    }

    /** Fetches the player's own gear configs, most recently updated first, for one-click tag tabs. */
    public void listOwnConfigs(Consumer<List<OwnedGearConfig>> onSuccess, Consumer<String> onFailure) {
        apiClient.get("/plugin/gear-configs",
                response -> {
                    OwnedGearConfigPage page = gson.fromJson(response.getBody(), OwnedGearConfigPage.class);
                    onSuccess.accept(page == null || page.getConfigs() == null ? List.of() : page.getConfigs());
                },
                response -> onFailure.accept(failureMessage(response)));
    }

    public void generate(String code, boolean includeMissing, Consumer<Result> onComplete) {
        apiClient.get("/plugin/gear/" + code,
                response -> build(response, includeMissing, onComplete),
                response -> onComplete.accept(Result.failure(failureMessage(response))));
    }

    private void build(ApiResponse response, boolean includeMissing, Consumer<Result> onComplete) {
        PluginGearData gear;
        try {
            gear = gson.fromJson(response.getBody(), PluginGearData.class);
        } catch (Exception exception) {
            gear = null;
        }
        if (gear == null || gear.getItemIds() == null || gear.getItemIds().isEmpty()) {
            onComplete.accept(Result.failure("That gear set has no items"));
            return;
        }

        PluginGearData gearData = gear;
        clientThread.invoke(() -> onComplete.accept(applyToBank(gearData, includeMissing)));
    }

    private Result applyToBank(PluginGearData gear, boolean includeMissing) {
        ItemContainer bank = client.getItemContainer(InventoryID.BANK);
        if (bank == null) {
            return Result.failure("Open your bank, then generate again");
        }

        Set<Integer> owned = new HashSet<>();
        Map<Integer, Integer> ownedByVariation = new HashMap<>();
        for (Item item : bank.getItems()) {
            if (item.getId() > 0 && item.getQuantity() > 0) {
                int itemId = itemManager.canonicalize(item.getId());
                owned.add(itemId);
                ownedByVariation.putIfAbsent(ItemVariationMapping.map(itemId), itemId);
            }
        }

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

        log.info("Gear tag '{}' generated: {} item(s), {} missing", tag, tagged.size(), missing.size());
        List<String> missingNames = new ArrayList<>();
        for (int itemId : missing) {
            missingNames.add(itemManager.getItemComposition(itemId).getName());
        }
        return new Result(true, summary(tag, tagged.size(), missing.size(), includeMissing), missingNames);
    }

    /**
     * The bank item to lay out for a config item: the exact item if the bank has it, otherwise another variation the
     * player owns (a different potion dose or poison level), otherwise -1. Tags are then added for that exact item
     * only, since a tag on every variation pulls the variations the config did not ask for into the tab as well.
     */
    static int bankItemFor(int itemId, Set<Integer> owned, Map<Integer, Integer> ownedByVariation,
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

    static String summary(String tag, int tagged, int missing, boolean includeMissing) {
        String base = "Tagged " + tagged + " item" + (tagged == 1 ? "" : "s") + " as \"" + tag + "\"";
        if (missing == 0) {
            return base + ".";
        }
        String items = missing + " item" + (missing == 1 ? "" : "s");
        return includeMissing
                ? base + ". " + items + " not in your bank, shown as placeholders."
                : base + ". " + items + " not in your bank " + (missing == 1 ? "was" : "were") + " left out.";
    }

    static String failureMessage(ApiResponse response) {
        switch (response.getStatusCode()) {
            case 404:
                return "No gear set found for that code";
            case 401:
                return "Check your API key in the plugin settings";
            case -1:
                return "Could not reach Lumbridge Guide";
            default:
                return "Generate failed (" + response.getStatusCode() + ")";
        }
    }
}
