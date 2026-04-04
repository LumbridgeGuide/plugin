package com.lumbridgeguide.data;

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
    private PluginTeamData myTeam;
    private List<PluginTileData> tiles;
}

