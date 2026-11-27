package com.lumbridgeguide.api;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResponse {

    public static final int NETWORK_ERROR = -1;

    private int statusCode;
    private String body;
}
