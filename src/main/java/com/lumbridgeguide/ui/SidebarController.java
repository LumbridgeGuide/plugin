package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.LumbridgeGuidePlugin;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import java.time.Duration;
import java.time.Instant;

@Singleton
public class SidebarController {

    private final LumbridgeGuideConfig config;
    private final ClientToolbar clientToolbar;
    private final BoardDataService boardDataService;
    private final TileProgressTracker tileProgressTracker;
    private final ProofService proofService;
    private final GearTagService gearTagService;
    private final GearConfigExportService gearConfigExportService;
    private final TripCheckService tripCheckService;
    private final AccountSyncService accountSyncService;
    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;
    private final LumbridgeGuideClient apiClient;

    private SidebarPanel panel;
    private NavigationButton navigationButton;
    private Instant lastBoardRefresh = Instant.now();

    @Inject
    public SidebarController(LumbridgeGuideConfig config, ClientToolbar clientToolbar,
                             BoardDataService boardDataService, TileProgressTracker tileProgressTracker,
                             ProofService proofService, GearTagService gearTagService,
                             GearConfigExportService gearConfigExportService, TripCheckService tripCheckService,
                             AccountSyncService accountSyncService, ItemManager itemManager,
                             SkillIconManager skillIconManager, LumbridgeGuideClient apiClient) {
        this.config = config;
        this.clientToolbar = clientToolbar;
        this.boardDataService = boardDataService;
        this.tileProgressTracker = tileProgressTracker;
        this.proofService = proofService;
        this.gearTagService = gearTagService;
        this.gearConfigExportService = gearConfigExportService;
        this.tripCheckService = tripCheckService;
        this.accountSyncService = accountSyncService;
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
        this.apiClient = apiClient;
    }

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
            boardDataService.refresh(this::refreshPanel);
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        int minutes = config.boardRefreshMinutes();
        if (panel == null || minutes <= 0
                || Duration.between(lastBoardRefresh, Instant.now()).toMinutes() < minutes) {
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
        if (event.getGameState() == GameState.LOGIN_SCREEN && panel != null) {
            panel.setBankOpen(false);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN && panel != null) {
            panel.setBankOpen(true);
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event) {
        if (event.getGroupId() == InterfaceID.BANKMAIN && panel != null) {
            panel.setBankOpen(false);
        }
    }

    private void addPanel() {
        panel = new SidebarPanel(boardDataService, tileProgressTracker, proofService, gearTagService,
                gearConfigExportService, tripCheckService, accountSyncService, itemManager, skillIconManager, config, apiClient::hasApiKey);
        navigationButton = NavigationButton.builder()
                .tooltip("Lumbridge Guide")
                .icon(ImageUtil.loadImageResource(LumbridgeGuidePlugin.class, "icon.png"))
                .priority(5)
                .panel(panel)
                .build();
        clientToolbar.addNavigation(navigationButton);
    }
}
