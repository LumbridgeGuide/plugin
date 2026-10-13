package com.lumbridgeguide.gear;

import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.gear.data.GearConfigCreatedData;
import com.lumbridgeguide.gear.data.GearConfigPayload;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/** Builds a website gear setup from the player's worn and carried items */
@Singleton
public class GearConfigExportService {

    private static final Set<Integer> RUNE_POUCHES = Set.of(ItemID.BH_RUNE_POUCH, ItemID.BH_RUNE_POUCH_TROUVER,
            ItemID.DIVINE_RUNE_POUCH, ItemID.DIVINE_RUNE_POUCH_TROUVER);

    private static final int[] POUCH_RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2,
            VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4
    };
    private static final int[] POUCH_AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2,
            VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4
    };

    private final Client client;
    private final ClientThread clientThread;
    private final ItemManager itemManager;
    private final LumbridgeGuideClient apiClient;

    @Inject
    public GearConfigExportService(Client client,
                                   ClientThread clientThread,
                                   ItemManager itemManager,
                                   LumbridgeGuideClient apiClient) {
        this.client = client;
        this.clientThread = clientThread;
        this.itemManager = itemManager;
        this.apiClient = apiClient;
    }

    public void create(String name, boolean includeEquipment, boolean includeInventory, Consumer<Result> onComplete) {
        Consumer<Result> onSwing = result -> SwingUtilities.invokeLater(() -> onComplete.accept(result));
        clientThread.invoke(() -> {
            if (client.getGameState() != GameState.LOGGED_IN) {
                onSwing.accept(Result.failure("Log in to the game first."));
                return;
            }
            apiClient.post("/plugin/gear-configs", payload(name, includeEquipment, includeInventory),
                    response -> onSwing.accept(Result.success(createdUrl(response))),
                    response -> onSwing.accept(Result.failure(
                            apiClient.failureMessage(response, "Could not create the gear config"))));
        });
    }

    private GearConfigPayload payload(String name, boolean includeEquipment, boolean includeInventory) {
        List<GearConfigPayload.InventoryEntry> inventory = includeInventory ? readInventory() : new ArrayList<>();
        boolean hasRunePouch = inventory.stream().anyMatch(entry -> RUNE_POUCHES.contains(entry.getItemId()));
        String configName = name == null || name.trim().isEmpty()
                ? defaultName()
                : name.trim();
        return new GearConfigPayload(
                configName,
                includeEquipment,
                includeInventory,
                includeEquipment ? readEquipment() : new ArrayList<>(),
                inventory,
                hasRunePouch ? readRunePouch() : new ArrayList<>());
    }

    private String createdUrl(ApiResponse response) {
        return apiClient.deserialize(response.getBody(), GearConfigCreatedData.class).getUrl();
    }

    List<GearConfigPayload.EquipmentEntry> readEquipment() {
        List<GearConfigPayload.EquipmentEntry> entries = new ArrayList<>();
        ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
        if (equipment == null) {
            return entries;
        }
        Item[] contents = equipment.getItems();
        for (EquipmentInventorySlot slot : EquipmentInventorySlot.values()) {
            int index = slot.getSlotIdx();
            if (index >= contents.length || contents[index].getId() <= 0 || contents[index].getQuantity() <= 0) {
                continue;
            }
            int itemId = itemManager.canonicalize(contents[index].getId());
            entries.add(new GearConfigPayload.EquipmentEntry(
                    slot.name(), itemId, itemName(itemId), contents[index].getQuantity()));
        }
        return entries;
    }

    List<GearConfigPayload.InventoryEntry> readInventory() {
        List<GearConfigPayload.InventoryEntry> entries = new ArrayList<>();
        ItemContainer inventory = client.getItemContainer(InventoryID.INV);
        if (inventory == null) {
            return entries;
        }
        Item[] contents = inventory.getItems();
        for (int slot = 0; slot < Math.min(contents.length, 28); slot++) {
            if (contents[slot].getId() <= 0 || contents[slot].getQuantity() <= 0) {
                continue;
            }
            int itemId = itemManager.canonicalize(contents[slot].getId());
            entries.add(new GearConfigPayload.InventoryEntry(
                    slot, itemId, itemName(itemId), contents[slot].getQuantity()));
        }
        return entries;
    }

    List<GearConfigPayload.RunePouchEntry> readRunePouch() {
        List<GearConfigPayload.RunePouchEntry> entries = new ArrayList<>();
        EnumComposition runes = client.getEnum(EnumID.RUNEPOUCH_RUNE);
        for (int index = 0; index < POUCH_RUNE_VARBITS.length; index++) {
            int runeType = client.getVarbitValue(POUCH_RUNE_VARBITS[index]);
            int amount = client.getVarbitValue(POUCH_AMOUNT_VARBITS[index]);
            if (runeType <= 0 || amount <= 0) {
                continue;
            }
            int itemId = runes.getIntValue(runeType);
            entries.add(new GearConfigPayload.RunePouchEntry(itemId, itemName(itemId), amount));
        }
        return entries;
    }

    private String defaultName() {
        String playerName = client.getLocalPlayer() == null ? null : client.getLocalPlayer().getName();
        return playerName == null ? "Gear setup" : Text.sanitize(playerName) + " setup";
    }

    private String itemName(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }

    @Value
    public static class Result {
        boolean success;
        String message;
        String url;

        static Result success(String url) {
            return new Result(true, "Gear config created.", url);
        }

        static Result failure(String message) {
            return new Result(false, message, null);
        }
    }
}
