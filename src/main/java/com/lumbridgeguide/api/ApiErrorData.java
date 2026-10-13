package com.lumbridgeguide.api;

import lombok.Data;

/** Error body the backend returns with a failed request */
@Data
public class ApiErrorData {
    private String error;
    private String message;
}
