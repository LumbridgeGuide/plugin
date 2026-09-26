package com.lumbridgeguide.gear.data;

import lombok.Data;

@Data
public class GearConfigCreatedData {
    private String id;
    private String name;
    private String url;
    private int flaggedItems;
}
