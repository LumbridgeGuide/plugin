package com.lumbridgeguide.account.data;

import lombok.Data;

@Data
public class AccountSyncResultData {
    private String status;
    private boolean newlyLinked;
}
