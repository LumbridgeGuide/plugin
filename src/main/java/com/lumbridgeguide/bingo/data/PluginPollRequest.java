package com.lumbridgeguide.bingo.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class PluginPollRequest {
    private Map<String, Long> boards;
    private Long cursor;
    private List<Long> ack;
}
