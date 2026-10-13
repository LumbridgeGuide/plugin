package com.lumbridgeguide.account;

import net.runelite.api.GameState;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tells a real login or logout apart from a world hop, which passes through loading states without logging out. A
 * login is reported on the next tick, once skills and quests have loaded
 */
final class LoginTracker {

    private static final Set<GameState> LOGGED_OUT_STATES = EnumSet.of(
            GameState.UNKNOWN,
            GameState.STARTING,
            GameState.LOGIN_SCREEN,
            GameState.LOGIN_SCREEN_AUTHENTICATOR,
            GameState.LOGGING_IN);

    private boolean awaitingLogin = true;
    private boolean loginPending;

    /** Called when the plugin starts, which may be while the player is already logged in */
    void start(boolean loggedIn) {
        awaitingLogin = true;
        loginPending = loggedIn;
    }

    /** Returns true when the state change is a logout */
    boolean onGameState(GameState state) {
        if (LOGGED_OUT_STATES.contains(state)) {
            boolean loggedOut = !awaitingLogin;
            if (loggedOut) {
                loginPending = false;
            }
            awaitingLogin = true;
            return loggedOut;
        }
        if (state == GameState.LOGGED_IN && awaitingLogin) {
            awaitingLogin = false;
            loginPending = true;
        }
        return false;
    }

    /** Returns true on the first tick after a login */
    boolean onTick() {
        boolean login = loginPending;
        loginPending = false;
        return login;
    }
}
