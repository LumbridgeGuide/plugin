package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginClaimData {
    private String teamId;
    private String teamName;
    private String teamColor;
    private String tileTitle;
    private String claimedAt;
}
