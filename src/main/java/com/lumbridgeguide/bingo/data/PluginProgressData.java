package com.lumbridgeguide.bingo.data;

import lombok.Data;

/** A teammate's progress on a kill count or XP tile, as their plugin last reported it. */
@Data
public class PluginProgressData {
    private String name;
    private long progress;
    private boolean mine;
}
