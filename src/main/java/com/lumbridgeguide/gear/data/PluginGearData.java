package com.lumbridgeguide.gear.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginGearData {
    private String id;
    private String name;
    private List<Integer> itemIds;
    /** Equipment slot name, such as "head" or "weapon", to item id. */
    private Map<String, Integer> equipment;
    /** The 28 inventory slots in order, null where a slot is empty. */
    private List<Integer> inventory;
    /** Rune pouch slots in order, null where a slot is empty. */
    private List<Integer> runePouch;
}
