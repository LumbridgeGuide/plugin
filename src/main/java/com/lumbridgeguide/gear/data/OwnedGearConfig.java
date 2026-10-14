package com.lumbridgeguide.gear.data;

import lombok.Data;

/** One of the player's own gear configs, as listed for one-click tag tabs. */
@Data
public class OwnedGearConfig {
    private String id;
    private String name;
    private int itemCount;
    private String updatedAt;
}
