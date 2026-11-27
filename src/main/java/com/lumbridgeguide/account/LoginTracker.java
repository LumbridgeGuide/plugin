package com.lumbridgeguide.account;

import net.runelite.api.GameState;

import java.util.EnumSet;
import java.util.Set;

public final class LoginTracker {

    private static final Set<GameState> LOGGED_OUT_STATES = EnumSet.of(
            GameState.UNKNOWN,
            GameState.STARTING,
            GameState.LOGIN_SCREEN,
            GameState.LOGIN_SCREEN_AUTHENTICATOR,
            GameState.LOGGING_IN);

    private boolean awaitingLogin = true;
    private boolean loginPending;

    public void start(boolean loggedIn) {
        loginPending = loggedIn;
        awaitingLogin = !loginPending;
    }

    public boolean onGameState(GameState state) {
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

    public boolean onTick() {
        boolean login = loginPending;
        loginPending = false;
        return login;
    }
}
