package com.lumbridgeguide.account.data;

import lombok.Value;

/** Asks the backend whether a game account is linked, by account hash */
@Value
public class AccountStatusRequest {
    long accountHash;
}
