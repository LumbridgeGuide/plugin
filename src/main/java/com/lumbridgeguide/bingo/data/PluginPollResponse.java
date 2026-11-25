package com.lumbridgeguide.bingo.data;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PluginPollResponse {
    private boolean changed;
    private long nextPollSeconds;
    private Long cursor;
    private Map<String, Long> versions;
    private List<PluginBoardData> boards;
    private List<PluginClaimEvent> claims;
    private List<PluginProofResult> results;
    private List<PluginBoardEvent> boardEvents;
}
