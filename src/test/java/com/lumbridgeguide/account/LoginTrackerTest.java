package com.lumbridgeguide.account;

import net.runelite.api.GameState;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoginTrackerTest {

    private final LoginTracker tracker = new LoginTracker();

    @Test
    public void reportsALoginOnTheNextTick() {
        tracker.start(false);

        assertFalse(tracker.onGameState(GameState.LOGGED_IN));
        assertTrue(tracker.onTick());
        assertFalse(tracker.onTick());
    }

    @Test
    public void treatsAWorldHopAsNeitherLogoutNorLogin() {
        tracker.start(false);
        tracker.onGameState(GameState.LOGGED_IN);
        tracker.onTick();

        assertFalse(tracker.onGameState(GameState.HOPPING));
        assertFalse(tracker.onGameState(GameState.LOADING));
        assertFalse(tracker.onGameState(GameState.LOGGED_IN));
        assertFalse(tracker.onTick());
    }

    @Test
    public void reportsALogoutOnce() {
        tracker.start(false);
        tracker.onGameState(GameState.LOGGED_IN);
        tracker.onTick();

        assertTrue(tracker.onGameState(GameState.LOGIN_SCREEN));
        assertFalse(tracker.onGameState(GameState.LOGIN_SCREEN));
    }

    @Test
    public void tracksAPlayerAlreadyLoggedInWhenThePluginStarts() {
        tracker.start(true);
        assertTrue(tracker.onTick());

        assertFalse(tracker.onGameState(GameState.LOADING));
        assertFalse(tracker.onGameState(GameState.LOGGED_IN));
        assertFalse(tracker.onTick());
        assertTrue(tracker.onGameState(GameState.LOGIN_SCREEN));
    }
}
