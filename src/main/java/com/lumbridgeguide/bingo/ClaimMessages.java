package com.lumbridgeguide.bingo;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ClaimMessages {
    ALL_TEAMS("All teams"),
    MY_TEAM("My team"),
    OFF("Off");

    private final String label;

    @Override
    public String toString() {
        return label;
    }
}
