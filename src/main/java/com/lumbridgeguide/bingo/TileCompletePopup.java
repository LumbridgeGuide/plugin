package com.lumbridgeguide.bingo;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WidgetNode;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetModalMode;
import net.runelite.api.widgets.WidgetUtil;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.function.BooleanSupplier;

/**
 * Announces a completed tile with the game's own notification popup, the one the collection log uses, so it shows in
 * the proof screenshot. It waits for any popup already showing, and closes its interface once the popup has animated
 * away.
 */
@Singleton
class TileCompletePopup {

    private static final int NOTIFICATION_DISPLAY_INIT = 3343;
    private static final int NOTIFICATION_INTERFACE = 660;
    private static final int NOTIFICATION_PANEL = 1;
    private static final int RESIZABLE_CLASSIC_SLOT = WidgetUtil.packComponentId(161, 13);
    private static final int RESIZABLE_MODERN_SLOT = WidgetUtil.packComponentId(164, 13);
    private static final int FIXED_SLOT = WidgetUtil.packComponentId(548, 42);
    private static final int TITLE_COLOUR = 0xFF981F;
    /** How long the popup takes to grow to full size before the screenshot is taken. */
    private static final long OPEN_ANIMATION_MILLIS = 1200;

    private final Client client;
    private final ClientThread clientThread;

    @Inject
    TileCompletePopup(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /** Shows the popup, then runs {@code onOpen} on the client thread once it is fully open. */
    void show(String title, String message, Runnable onOpen) {
        clientThread.invokeLater(new BooleanSupplier() {
            private long openedAt = -1;
            private WidgetNode node;

            @Override
            public boolean getAsBoolean() {
                if (client.getGameState() != GameState.LOGGED_IN) {
                    return true;
                }
                if (openedAt < 0) {
                    if (client.getWidget(NOTIFICATION_INTERFACE, NOTIFICATION_PANEL) != null) {
                        return false;
                    }
                    node = client.openInterface(slot(), NOTIFICATION_INTERFACE, WidgetModalMode.MODAL_CLICKTHROUGH);
                    client.runScript(NOTIFICATION_DISPLAY_INIT, title, message, TITLE_COLOUR);
                    openedAt = System.currentTimeMillis();
                    return false;
                }
                if (System.currentTimeMillis() - openedAt < OPEN_ANIMATION_MILLIS) {
                    return false;
                }
                onOpen.run();
                closeWhenFinished(node);
                return true;
            }
        });
    }

    private void closeWhenFinished(WidgetNode node) {
        clientThread.invokeLater(() -> {
            Widget panel = client.getWidget(NOTIFICATION_INTERFACE, NOTIFICATION_PANEL);
            if (panel != null && panel.getWidth() > 0) {
                return false;
            }
            client.closeInterface(node, true);
            return true;
        });
    }

    private int slot() {
        if (!client.isResized()) {
            return FIXED_SLOT;
        }
        return client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
                ? RESIZABLE_MODERN_SLOT
                : RESIZABLE_CLASSIC_SLOT;
    }
}
