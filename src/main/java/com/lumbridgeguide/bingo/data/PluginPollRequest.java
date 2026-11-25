package com.lumbridgeguide.bingo.data;

import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
public class PluginPollRequest {
    Map<String, Long> boards;
    Long cursor;
    List<Long> ack;
}
