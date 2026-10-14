package com.lumbridgeguide.bingo.data;

import lombok.Data;

import java.util.List;

/** Active bingo boards returned by the plugin sync endpoint */
@Data
public class PluginSyncResponse {
    private List<PluginBoardData> boards;
}

