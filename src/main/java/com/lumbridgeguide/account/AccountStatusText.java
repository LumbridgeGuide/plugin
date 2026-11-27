package com.lumbridgeguide.account;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AccountStatusText {

    public static final StatusLine LOGGED_OUT =
            new StatusLine("Log in to sync this account", "", StatusTone.MUTED, AccountAction.NONE);

    public static StatusLine forStatus(String status) {
        if (status == null) {
            return new StatusLine("Link not checked", "", StatusTone.MUTED, AccountAction.REFRESH);
        }
        switch (status) {
            case "LINKED":
                return new StatusLine("Linked", "", StatusTone.SUCCESS, AccountAction.SYNC);
            case "NOT_LINKED":
                return new StatusLine("Not linked", "", StatusTone.MUTED, AccountAction.LINK);
            case "LIMIT_REACHED":
                return new StatusLine("Account limit reached", "Manage your accounts on the website",
                        StatusTone.WARNING, AccountAction.REFRESH);
            case "LOCKED":
                return new StatusLine("Account locked", "Over your plan's account limit. Manage it on the website",
                        StatusTone.WARNING, AccountAction.REFRESH);
            case "LINKED_ELSEWHERE":
                return new StatusLine("Linked elsewhere", "This account is linked to another Lumbridge Guide user",
                        StatusTone.ERROR, AccountAction.REFRESH);
            default:
                return new StatusLine("Unknown link status", "", StatusTone.MUTED, AccountAction.REFRESH);
        }
    }

    public static String impliedLinkStatus(String errorCode) {
        if (errorCode == null) {
            return null;
        }
        switch (errorCode) {
            case "ACCOUNT_LINKED_ELSEWHERE":
                return "LINKED_ELSEWHERE";
            case "ACCOUNT_LIMIT_REACHED":
                return "LIMIT_REACHED";
            case "ACCOUNT_LOCKED":
                return "LOCKED";
            case "ACCOUNT_NOT_LINKED":
                return "NOT_LINKED";
            default:
                return null;
        }
    }
}
