package com.lumbridgeguide.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginGearData {
    private String id;
    private String name;
    private List<Integer> itemIds;
}
