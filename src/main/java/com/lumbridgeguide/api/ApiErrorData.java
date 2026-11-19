package com.lumbridgeguide.api;

import lombok.Data;

@Data
public class ApiErrorData {
    private String error;
    private String message;
}
