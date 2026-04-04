package com.lumbridgeguide.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginTileData {
    private String id;
    private int position;
    private String type;
    private String title;
    private String description;
    private int points;
    private String skill;
    private long xpTarget;
    private List<TileItemEntry> items;
    private Integer monsterId;
    private String monsterName;
    private Integer killCount;
    private boolean claimed;
    private String claimedByTeamId;
}

