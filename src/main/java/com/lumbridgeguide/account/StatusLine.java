package com.lumbridgeguide.account;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatusLine {
    private String headline;
    private String detail;
    private StatusTone tone;
    private AccountAction action;
}
