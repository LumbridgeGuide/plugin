package com.lumbridgeguide.gear.data;

import lombok.Data;

import java.util.List;

@Data
public class OwnedGearConfigPage {
    private List<OwnedGearConfig> configs;
    private long totalCount;
}
