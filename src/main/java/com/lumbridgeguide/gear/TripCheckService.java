package com.lumbridgeguide.gear;

import com.google.gson.Gson;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.gear.data.GearConfigPayload;
import com.lumbridgeguide.gear.data.PluginGearData;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;

/** Compares what the player wears and carries with one of their gear configs, before they leave the bank. */
@Singleton
public class TripCheckService {

    /** Where a missing item is: in the bank, not owned, or unknown because the bank has not been opened yet. */
    public enum Whereabouts {
        IN_BANK,
        NOT_OWNED,
        UNKNOWN
    }

    @Value
    public static class Missing {
        String name;
        int count;
        Whereabouts whereabouts;
    }

    @Value
    public static class Check {
        String name;
        int wornHave;
        int wornTotal;
        int inventoryHave;
        int inventoryTotal;
        int pouchHave;
        int pouchTotal;
        List<Missing> missing;

        public int have() {
            return wornHave + inventoryHave + pouchHave;
        }

        public int total() {
            return wornTotal + inventoryTotal + pouchTotal;
        }
    }

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;
    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final GearConfigExportService exportService;

    @Inject
    public TripCheckService(LumbridgeGuideClient apiClient, Gson gson, Client client, ClientThread clientThread,
                            ItemManager itemManager, GearConfigExportService exportService) {
        this.apiClient = apiClient;
        this.gson = gson;
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        this.exportService = exportService;
    }

    public void check(String configId, Consumer<Check> onSuccess, Consumer<String> onFailure) {
        apiClient.get("/plugin/gear/" + configId,
                response -> {
                    PluginGearData gear = gson.fromJson(response.getBody(), PluginGearData.class);
                    if (gear == null) {
                        onFailure.accept("That gear set has no items");
                        return;
                    }
                    clientThread.invoke(() -> onSuccess.accept(checkOnClientThread(gear)));
                },
                response -> onFailure.accept(GearTagService.failureMessage(response)));
    }

    private Check checkOnClientThread(PluginGearData gear) {
        List<Integer> worn = exportService.readEquipment().stream()
                .map(GearConfigPayload.EquipmentEntry::getItemId).collect(Collectors.toList());
        List<Integer> inventory = exportService.readInventory().stream()
                .map(GearConfigPayload.InventoryEntry::getItemId).collect(Collectors.toList());
        List<Integer> pouch = exportService.readRunePouch().stream()
                .map(GearConfigPayload.RunePouchEntry::getItemId).collect(Collectors.toList());
        Set<Integer> bank = null;
        ItemContainer bankContainer = client.getItemContainer(InventoryID.BANK);
        if (bankContainer != null) {
            bank = new HashSet<>();
            for (Item item : bankContainer.getItems()) {
                if (item.getId() > 0 && item.getQuantity() > 0) {
                    bank.add(ItemVariationMapping.map(itemManager.canonicalize(item.getId())));
                }
            }
        }
        return compare(gear, worn, inventory, pouch, bank, ItemVariationMapping::map,
                itemId -> itemManager.getItemComposition(itemId).getName());
    }

    /**
     * Counts each required item once per slot it fills, matching item variations (such as charged and uncharged
     * versions) as the same item. The bank is null when it has not been opened this session.
     */
    static Check compare(PluginGearData gear, List<Integer> worn, List<Integer> inventory, List<Integer> pouch,
                         Set<Integer> bank, IntUnaryOperator variation, IntFunction<String> names) {
        Map<Integer, Integer> missingCounts = new LinkedHashMap<>();
        Collection<Integer> wornRequired = gear.getEquipment() == null ? List.of() : gear.getEquipment().values();
        int[] wornResult = tally(wornRequired, worn, variation, missingCounts);
        int[] inventoryResult = tally(orEmpty(gear.getInventory()), inventory, variation, missingCounts);
        int[] pouchResult = tally(orEmpty(gear.getRunePouch()), pouch, variation, missingCounts);

        List<Missing> missing = new ArrayList<>();
        missingCounts.forEach((itemId, count) -> {
            Whereabouts whereabouts = bank == null ? Whereabouts.UNKNOWN
                    : bank.contains(variation.applyAsInt(itemId)) ? Whereabouts.IN_BANK : Whereabouts.NOT_OWNED;
            missing.add(new Missing(names.apply(itemId), count, whereabouts));
        });
        return new Check(gear.getName(), wornResult[0], wornResult[1], inventoryResult[0], inventoryResult[1],
                pouchResult[0], pouchResult[1], missing);
    }

    private static int[] tally(Collection<Integer> required, List<Integer> carried, IntUnaryOperator variation,
                               Map<Integer, Integer> missingCounts) {
        Map<Integer, Integer> available = new HashMap<>();
        for (Integer itemId : carried) {
            available.merge(variation.applyAsInt(itemId), 1, Integer::sum);
        }
        int have = 0;
        int total = 0;
        for (Integer itemId : required) {
            if (itemId == null || itemId <= 0) {
                continue;
            }
            total++;
            int key = variation.applyAsInt(itemId);
            int left = available.getOrDefault(key, 0);
            if (left > 0) {
                available.put(key, left - 1);
                have++;
            } else {
                missingCounts.merge(itemId, 1, Integer::sum);
            }
        }
        return new int[]{have, total};
    }

    private static List<Integer> orEmpty(List<Integer> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}
