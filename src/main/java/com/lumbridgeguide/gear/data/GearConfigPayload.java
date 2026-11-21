package com.lumbridgeguide.gear.data;

import lombok.Value;

import java.util.List;

@Value
public class GearConfigPayload {
    String name;
    boolean includeEquipment;
    boolean includeInventory;
    List<EquipmentEntry> equipment;
    List<InventoryEntry> inventory;
    List<RunePouchEntry> runePouch;

    @Value
    public static class EquipmentEntry {
        String slot;
        int itemId;
        String name;
        int quantity;
    }

    @Value
    public static class InventoryEntry {
        int slot;
        int itemId;
        String name;
        int quantity;
    }

    @Value
    public static class RunePouchEntry {
        int itemId;
        String name;
        int quantity;
    }
}
