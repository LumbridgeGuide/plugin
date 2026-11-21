package com.lumbridgeguide.bingo.data;

import lombok.Data;

import java.util.List;

@Data
public class PluginSyncResponse {
    private List<PluginBoardData> boards;
}

