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
    /** Team ranks, or null while a hidden board keeps other teams' claims secret. */
    private List<PluginStandingData> standings;
    /** The latest claims, newest first, or null while a hidden board keeps them secret. */
    private List<PluginClaimData> recentClaims;
}

