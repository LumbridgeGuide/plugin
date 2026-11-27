package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.LumbridgeGuidePlugin;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.TileProgressTracker;
import lombok.RequiredArgsConstructor;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class SidebarController {

    private final LumbridgeGuideConfig config;
    private final ClientToolbar clientToolbar;
    private final BoardDataService boardDataService;
    private final Provider<SidebarPanel> panelProvider;

    private SidebarPanel panel;
    private NavigationButton navigationButton;
    private Instant lastBoardRefresh = Instant.now();
    private boolean bankOpen;

    public void startUp() {
        Theme.setAccent(config.accentColour());
        addPanel();
        boardDataService.refresh(this::refreshPanel);
        lastBoardRefresh = Instant.now();
    }

    public void shutDown() {
        clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!LumbridgeGuideConfig.CONFIG_GROUP.equals(event.getGroup()) || panel == null
                || event.getKey().startsWith(TileProgressTracker.BASELINE_KEY_PREFIX)) {
            return;
        }

        if (LumbridgeGuideConfig.ACCENT_COLOUR_KEY.equals(event.getKey())) {
            Theme.setAccent(config.accentColour());
            SwingUtilities.invokeLater(() -> {
                clientToolbar.removeNavigation(navigationButton);
                addPanel();
            });
            return;
        }

        panel.refresh();
        if (LumbridgeGuideConfig.API_KEY_KEY.equals(event.getKey())) {
            boardDataService.clear();
            boardDataService.refresh(this::refreshPanel);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (panel == null
                || Duration.between(lastBoardRefresh, Instant.now()).getSeconds() < boardDataService.getNextPollSeconds()) {
            return;
        }

        lastBoardRefresh = Instant.now();
        boardDataService.refresh(this::refreshPanel);
    }

    private void refreshPanel() {
        SidebarPanel current = panel;
        if (current != null) {
            current.refresh();
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            setBankOpen(false);
            lastBoardRefresh = Instant.EPOCH;
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            setBankOpen(true);
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            setBankOpen(false);
        }
    }

    private void setBankOpen(boolean open) {
        bankOpen = open;
        if (panel != null) {
            panel.setBankOpen(open);
        }
    }

    private void addPanel() {
        panel = panelProvider.get();
        panel.setBankOpen(bankOpen);
        navigationButton = NavigationButton.builder()
                .tooltip("Lumbridge Guide")
                .icon(ImageUtil.loadImageResource(LumbridgeGuidePlugin.class, "icon.png"))
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navigationButton);
    }
}
