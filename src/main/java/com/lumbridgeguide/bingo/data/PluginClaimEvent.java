package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginClaimEvent {
    private long id;
    private String boardId;
    private String boardTitle;
    private String tileTitle;
    private String teamId;
    private String teamName;
}
