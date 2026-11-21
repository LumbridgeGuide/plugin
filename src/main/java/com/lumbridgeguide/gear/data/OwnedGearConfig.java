package com.lumbridgeguide.gear.data;

import lombok.Data;

@Data
public class OwnedGearConfig {
    private String id;
    private String name;
    private int itemCount;
    private String updatedAt;
}
