package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class PluginProgressData {
    private String name;
    private long progress;
    private boolean mine;
}
