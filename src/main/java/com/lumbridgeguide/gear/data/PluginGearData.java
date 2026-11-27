package com.lumbridgeguide.gear.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PluginGearData {
    private String name;
    private List<Integer> itemIds;
    private Map<String, Integer> equipment;
    private List<Integer> inventory;
    private List<Integer> runePouch;
}
