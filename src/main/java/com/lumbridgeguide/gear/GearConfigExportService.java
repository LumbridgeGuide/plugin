package com.lumbridgeguide.gear;

import com.lumbridgeguide.api.ApiErrorData;
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
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class GearConfigExportService {

    private static final int RUNE_POUCH_ID = 12791;
    private static final int DIVINE_RUNE_POUCH_ID = 27281;

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
        clientThread.invoke(() -> {
            if (client.getGameState() != GameState.LOGGED_IN) {
                onComplete.accept(Result.failure("Log in to the game first."));
                return;
            }
            List<GearConfigPayload.InventoryEntry> inventory = includeInventory ? readInventory() : new ArrayList<>();
            boolean hasRunePouch = inventory.stream()
                    .anyMatch(entry -> entry.getItemId() == RUNE_POUCH_ID || entry.getItemId() == DIVINE_RUNE_POUCH_ID);
            String configName = name == null || name.trim().isEmpty()
                    ? defaultName()
                    : name.trim();
            GearConfigPayload payload = new GearConfigPayload(
                    configName,
                    includeEquipment,
                    includeInventory,
                    includeEquipment ? readEquipment() : new ArrayList<>(),
                    inventory,
                    hasRunePouch ? readRunePouch() : new ArrayList<>());

            apiClient.post("/plugin/gear-configs", payload,
                    response -> {
                        GearConfigCreatedData created =
                                apiClient.deserialize(response.getBody(), GearConfigCreatedData.class);
                        onComplete.accept(Result.success(created.getUrl()));
                    },
                    response -> onComplete.accept(Result.failure(failureMessage(response))));
        });
    }

    private List<GearConfigPayload.EquipmentEntry> readEquipment() {
        List<GearConfigPayload.EquipmentEntry> entries = new ArrayList<>();
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
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

    private List<GearConfigPayload.InventoryEntry> readInventory() {
        List<GearConfigPayload.InventoryEntry> entries = new ArrayList<>();
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
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

    private List<GearConfigPayload.RunePouchEntry> readRunePouch() {
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
        return playerName == null ? "Gear setup" : playerName.replace('\u00A0', ' ') + " setup";
    }

    private String itemName(int itemId) {
        return itemManager.getItemComposition(itemId).getName();
    }

    private String failureMessage(ApiResponse response) {
        if (response.getStatusCode() == -1) {
            return "Could not reach Lumbridge Guide.";
        }
        if (response.getStatusCode() == 401) {
            return "Check your API key in the plugin settings.";
        }
        try {
            ApiErrorData error = apiClient.deserialize(response.getBody(), ApiErrorData.class);
            if (error != null && error.getMessage() != null) {
                return error.getMessage();
            }
        } catch (RuntimeException exception) {
            return "Could not create the gear config (" + response.getStatusCode() + ").";
        }
        return "Could not create the gear config (" + response.getStatusCode() + ").";
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
