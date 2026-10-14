package com.lumbridgeguide.gear.data;

import lombok.Data;

/** Backend's reply after creating a gear setup, with its website link */
@Data
public class GearConfigCreatedData {
    private String id;
    private String name;
    private String url;
    private int flaggedItems;
}
