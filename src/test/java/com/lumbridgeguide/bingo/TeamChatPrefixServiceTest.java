package com.lumbridgeguide.bingo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void swapsThePrefixWhenTheSelectedBoardChanges() {
        assertEquals("[Blue] Bob: hi*",
                TeamChatPrefixService.withPrefix("[Red] Bob: hi*", "Bob", "[Red] ", "[Blue] "));
    }

    @Test
    public void removesThePrefixWhenTheBoardHasNoTeam() {
        assertEquals("Bob: hi*", TeamChatPrefixService.withPrefix("[Red] Bob: hi*", "Bob", "[Red] ", null));
    }
}
