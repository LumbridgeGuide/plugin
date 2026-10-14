package com.lumbridgeguide.api;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Status code and raw body of one finished backend request */
@Data
@AllArgsConstructor
public class ApiResponse {
    private final int statusCode;
    private final String body;

    public static ApiResponse error(int statusCode, String message) {
        return new ApiResponse(statusCode, message);
    }
}
