package com.lumbridgeguide.bingo.data;

import lombok.Data;

import java.util.List;

@Data
public class PluginBoardData {
    private String id;
    private String title;
    private String description;
    private int gridSize;
    private String startsAt;
    private String endsAt;
    private String verificationCode;
    private boolean tilePointsEnabled;
    private String webUrl;
    private PluginTeamData myTeam;
    private List<PluginTileData> tiles;
    /** Null when the player has no team or the board hides standings during play */
    private PluginTeamSummary teamSummary;

    public List<PluginTileData> tiles() {
        return tiles == null ? List.of() : tiles;
    }
}

