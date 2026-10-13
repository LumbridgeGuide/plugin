package com.lumbridgeguide.account.data;

import lombok.Data;

/** Backend's reply after syncing, including whether a new account was linked */
@Data
public class AccountSyncResultData {
    private String status;
    private String displayName;
    private boolean newlyLinked;
    private int unmatchedQuests;
}
