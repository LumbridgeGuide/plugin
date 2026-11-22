package com.lumbridgeguide.account;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountStatusText {

    public enum Tone {
        SUCCESS,
        WARNING,
        ERROR,
        MUTED
    }

    @Value
    static class Line {
        String headline;
        String detail;
        Tone tone;
        AccountAction action;
    }

    static final Line LOGGED_OUT = new Line("Log in to sync this account", "", Tone.MUTED, AccountAction.NONE);

    static Line forStatus(String status) {
        if (status == null) {
            return new Line("Checking link status...", "", Tone.MUTED, AccountAction.NONE);
        }
        switch (status) {
            case "LINKED":
                return new Line("Linked", "", Tone.SUCCESS, AccountAction.SYNC);
            case "NOT_LINKED":
                return new Line("Not linked", "", Tone.MUTED, AccountAction.LINK);
            case "LIMIT_REACHED":
                return new Line("Account limit reached", "Manage your accounts on the website.", Tone.WARNING,
                        AccountAction.REFRESH);
            case "LOCKED":
                return new Line("Account locked", "Locked on your current plan. Manage it on the website.",
                        Tone.WARNING, AccountAction.REFRESH);
            case "LINKED_ELSEWHERE":
                return new Line("Linked elsewhere", "This account is linked to another Lumbridge Guide user.",
                        Tone.ERROR, AccountAction.REFRESH);
            default:
                return new Line("Unknown link status", "", Tone.MUTED, AccountAction.REFRESH);
        }
    }

    static String impliedLinkStatus(String errorCode) {
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

    static String accountTypeForIronmanVarbit(int ironmanVarbit) {
        switch (ironmanVarbit) {
            case 1:
                return "IRONMAN";
            case 2:
                return "ULTIMATE_IRONMAN";
            case 3:
                return "HARDCORE_IRONMAN";
            case 4:
                return "GROUP_IRONMAN";
            case 5:
                return "HARDCORE_GROUP_IRONMAN";
            case 6:
                return "UNRANKED_GROUP_IRONMAN";
            default:
                return "MAIN";
        }
    }
}
