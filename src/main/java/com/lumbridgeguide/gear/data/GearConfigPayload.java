package com.lumbridgeguide.gear.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GearConfigPayload {
    private String name;
    private boolean includeEquipment;
    private boolean includeInventory;
    private List<EquipmentEntry> equipment;
    private List<InventoryEntry> inventory;
    private List<RunePouchEntry> runePouch;
}
