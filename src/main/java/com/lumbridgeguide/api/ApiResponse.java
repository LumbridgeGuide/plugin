package com.lumbridgeguide.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {
    private final int statusCode;
    private final String body;
    private final boolean success;

    public static ApiResponse error(int statusCode, String message) {
        return new ApiResponse(statusCode, message, false);
    }
}
