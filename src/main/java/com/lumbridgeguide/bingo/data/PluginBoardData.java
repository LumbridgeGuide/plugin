package com.lumbridgeguide.bingo.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

