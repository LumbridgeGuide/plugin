package com.lumbridgeguide.bingo.data;

import lombok.Data;

import java.util.List;

@Data
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
    private String claimedByTeamName;
    /** The viewer's team only, most first */
    private List<PluginProgressData> progress;
    /** PENDING, APPROVED, REJECTED or AUTO_CLAIMED, or null when the team has sent none */
    private String proofStatus;

    public boolean isProofPending() {
        return "PENDING".equals(proofStatus);
    }

    public boolean isProofRejected() {
        return "REJECTED".equals(proofStatus);
    }
}

