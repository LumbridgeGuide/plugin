package com.lumbridgeguide.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TeamChatPrefixTest {

    @Test
    public void colorsAPlainTeamName() {
        assertEquals("<col=ef4444>[Red Team]</col> ", TeamChatPrefix.of("Red Team", "#ef4444"));
    }

    @Test
    public void escapesMarkupInTheTeamName() {
        assertEquals(
                "<col=ef4444>[<lt>img=0<gt> Mods<lt>/col<gt>]</col> ",
                TeamChatPrefix.of("<img=0> Mods</col>", "#ef4444"));
    }

    @Test
    public void dropsTheColourWhenItIsNotHex() {
        assertEquals("[Red Team] ", TeamChatPrefix.of("Red Team", "ff0000><img=1"));
        assertEquals("[Red Team] ", TeamChatPrefix.of("Red Team", null));
    }
}
