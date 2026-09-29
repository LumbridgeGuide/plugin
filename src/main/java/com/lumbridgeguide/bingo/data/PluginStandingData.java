package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginStandingData {
    private String teamId;
    private String name;
    private String color;
    private int score;
    private int rank;
}
