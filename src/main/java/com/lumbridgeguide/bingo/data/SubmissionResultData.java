package com.lumbridgeguide.bingo.data;

import lombok.Data;

@Data
public class SubmissionResultData {
    private String id;
    private String status;
    private boolean claimed;
    private String message;
}
