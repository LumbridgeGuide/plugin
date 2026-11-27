package com.lumbridgeguide.account.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountStatusRequest {
    private long accountHash;
}
