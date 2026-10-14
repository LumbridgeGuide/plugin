package com.lumbridgeguide.account.data;

import lombok.Data;

/** Backend's link state and display name for one game account */
@Data
public class AccountStatusData {
    private String status;
    private String displayName;
}
