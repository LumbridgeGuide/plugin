package com.lumbridgeguide.account;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Maps the backend's link statuses and error codes to what the sidebar shows. Kept free of game and Swing types so
 * it can be unit tested.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AccountStatusText {

    public enum Tone {
        SUCCESS,
        WARNING,
        ERROR,
        MUTED
    }

    @Getter
    @AllArgsConstructor
    static final class Line {
        private final String text;
        private final Tone tone;
    }

    static Line forStatus(String status) {
        if (status == null) {
            return new Line("Checking link status...", Tone.MUTED);
        }
        switch (status) {
            case "LINKED":
                return new Line("Linked to your Lumbridge Guide account", Tone.SUCCESS);
            case "NOT_LINKED":
                return new Line("Not linked. Press Sync now to link this account.", Tone.MUTED);
            case "LIMIT_REACHED":
                return new Line("Account limit reached. Manage your accounts on the website.", Tone.WARNING);
            case "LOCKED":
                return new Line("Locked on your current plan. Manage it on the website.", Tone.WARNING);
            case "LINKED_ELSEWHERE":
                return new Line("Linked to another Lumbridge Guide user.", Tone.ERROR);
            default:
                return new Line("Unknown link status", Tone.MUTED);
        }
    }

    /** The status an error code implies, or null when the error says nothing about the link. */
    static String statusForError(String errorCode) {
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

    /** The account type as the website words it, such as "Hardcore ironman". Regular accounts are "Main". */
    static String accountTypeLabel(String accountType) {
        if (accountType == null) {
            return "";
        }
        switch (accountType) {
            case "IRONMAN":
                return "Ironman";
            case "ULTIMATE_IRONMAN":
                return "Ultimate ironman";
            case "HARDCORE_IRONMAN":
                return "Hardcore ironman";
            case "GROUP_IRONMAN":
                return "Group ironman";
            case "HARDCORE_GROUP_IRONMAN":
                return "Hardcore group ironman";
            case "UNRANKED_GROUP_IRONMAN":
                return "Unranked group ironman";
            default:
                return "Main";
        }
    }

    /** A short badge for the link status, such as LINKED, or an empty string while it is unknown. */
    static String statusBadge(String status) {
        return status == null ? "" : status.replace('_', ' ');
    }

    /** RuneLite's IRONMAN varbit value to the backend's account type. */
    static String accountType(int ironmanVarbit) {
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
                return "REGULAR";
        }
    }
}
