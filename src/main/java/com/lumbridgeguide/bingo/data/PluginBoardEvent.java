package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginBoardEvent {
    private BoardEventType type;
    private String boardTitle;
}
