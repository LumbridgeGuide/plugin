package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginTeamSummary {
    /** Null until the team has scored */
    private Integer rank;
    private int teamCount;
    private int score;
}
