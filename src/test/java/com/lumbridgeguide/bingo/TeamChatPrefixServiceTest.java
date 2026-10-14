package com.lumbridgeguide.bingo;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TeamChatPrefixServiceTest {

    @Test
    public void matchesAnIronAccountsOwnChatName() {
        assertTrue(TeamChatPrefixService.isOwnName("Iron Bob", "<img=2>Iron\u00A0Bob"));
    }

    @Test
    public void doesNotMatchAnotherPlayer() {
        assertFalse(TeamChatPrefixService.isOwnName("Iron Bob", "<img=2>Iron\u00A0Rob"));
    }
}
