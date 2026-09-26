package com.lumbridgeguide.api;

import lombok.Data;

/** The backend's error body. {@code error} is a stable code such as ACCOUNT_LIMIT_REACHED. */
@Data
public class ApiErrorData {
    private String error;
    private String message;
}
