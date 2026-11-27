package com.lumbridgeguide.gear;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GearResult {
    private boolean success;
    private String message;
    private String url;
    private List<String> missingItems;

    public static GearResult failure(String message) {
        return new GearResult(false, message, null, List.of());
    }
}
