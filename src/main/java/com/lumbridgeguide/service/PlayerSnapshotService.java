package com.lumbridgeguide.service;

import com.lumbridgeguide.data.EquipmentItemData;
import com.lumbridgeguide.data.InventoryItemData;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Captures a snapshot of different aspects of the players state
 * Must be called on the client thread
 */
@Singleton
public class PlayerSnapshotService {

    private final Client client;
    private final ItemManager itemManager;

    @Inject
    public PlayerSnapshotService(Client client, ItemManager itemManager) {
        this.client = client;
        this.itemManager = itemManager;
    }

    public List<InventoryItemData> readInventoryItems() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory == null) {
            return Collections.emptyList();
        }

        Item[] contents = inventory.getItems();
        List<InventoryItemData> items = new ArrayList<>();

        for (int slot = 0; slot < contents.length; slot++) {
            Item item = contents[slot];
            if (item.getId() <= 0 || item.getQuantity() <= 0) {
                continue;
            }

            ItemComposition composition = itemManager.getItemComposition(item.getId());
            items.add(InventoryItemData.builder()
                    .itemId(item.getId())
                    .name(composition.getName())
                    .quantity(item.getQuantity())
                    .slot(slot)
                    .build());
        }

        return Collections.unmodifiableList(items);
    }

    public List<EquipmentItemData> readEquipmentItems() {
        ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);
        if (equipment == null) {
            return Collections.emptyList();
        }

        Item[] contents = equipment.getItems();
        List<EquipmentItemData> items = new ArrayList<>();

        for (EquipmentInventorySlot equipmentSlot : EquipmentInventorySlot.values()) {
            int slotIndex = equipmentSlot.getSlotIdx();
            if (slotIndex < 0 || slotIndex >= contents.length) {
                continue;
            }

            Item item = contents[slotIndex];
            if (item.getId() <= 0 || item.getQuantity() <= 0) {
                continue;
            }

            ItemComposition composition = itemManager.getItemComposition(item.getId());
            items.add(EquipmentItemData.builder()
                    .itemId(item.getId())
                    .name(composition.getName())
                    .quantity(item.getQuantity())
                    .slot(equipmentSlot.name())
                    .build());
        }

        return Collections.unmodifiableList(items);
    }

    public String getPlayerName() {
        if (client.getLocalPlayer() != null) {
            return client.getLocalPlayer().getName();
        }
        return "Unknown";
    }
}

