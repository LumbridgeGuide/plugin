package com.lumbridgeguide.account;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountView {
    private String displayName;
    private String headline;
    private String detail;
    private StatusTone tone;
    private AccountAction action;
    private String message;
    private StatusTone messageTone;
    private String lastSynced;
    private boolean busy;
}
