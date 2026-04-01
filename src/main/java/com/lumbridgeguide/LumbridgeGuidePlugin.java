package com.lumbridgeguide;

import com.google.inject.Provides;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.data.InventoryItemData;
import com.lumbridgeguide.data.SyncRequest;
import com.lumbridgeguide.service.PlayerSnapshotService;
import com.lumbridgeguide.ui.LumbridgeGuideTheme;
import com.lumbridgeguide.ui.LumbridgeGuidePanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Lumbridge Guide"
)
public class LumbridgeGuidePlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private LumbridgeGuideConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private LumbridgeGuideClient apiClient;

    @Inject
    private PlayerSnapshotService snapshotService;

    private LumbridgeGuidePanel panel;
    private NavigationButton navigationButton;

    @Override
    protected void startUp() throws Exception {
        log.info("Lumbridge Guide started");

        panel = new LumbridgeGuidePanel(this, config);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

        navigationButton = NavigationButton.builder()
                .tooltip("Lumbridge Guide")
                .icon(icon)
                .priority(5)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navigationButton);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Lumbridge Guide stopped");
        clientToolbar.removeNavigation(navigationButton);
        panel = null;
        navigationButton = null;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (LumbridgeGuideConfig.CONFIG_GROUP.equals(event.getGroup()) && panel != null) {
            panel.refresh();
        }
    }

    /**
     * Send a sync request to the test endpoint
     */
    public void onSyncButtonClicked() {
        if (!apiClient.isAuthenticated()) {
            panel.setStatus("API key not set!", LumbridgeGuideTheme.STATUS_ERROR);
            return;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            panel.setStatus("You must be logged in!", LumbridgeGuideTheme.STATUS_ERROR);
            return;
        }

        panel.setSyncing(true);
        panel.setStatus("Reading inventory...", LumbridgeGuideTheme.STATUS_PENDING);

        clientThread.invokeLater(() ->
        {
            List<InventoryItemData> items = snapshotService.readInventoryItems();
            String playerName = snapshotService.getPlayerName();

            SyncRequest syncRequest = SyncRequest.builder()
                    .playerName(playerName)
                    .items(items)
                    .build();

            panel.setStatus("Sending to server...", LumbridgeGuideTheme.STATUS_PENDING);

            apiClient.post("/test", syncRequest,
                    response ->
                    {
                        panel.setStatus("Synced! (" + items.size() + " items)", LumbridgeGuideTheme.STATUS_SUCCESS);
                        panel.setSyncing(false);
                        log.info("Inventory synced: {} items", items.size());
                    },
                    error ->
                    {
                        String errorMessage = error.getStatusCode() == -1
                                ? "Connection failed"
                                : "Error " + error.getStatusCode();
                        panel.setStatus(errorMessage, LumbridgeGuideTheme.STATUS_ERROR);
                        panel.setSyncing(false);
                        log.warn("Inventory sync failed: status {}", error.getStatusCode());
                    });
        });
    }

    @Provides
    LumbridgeGuideConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LumbridgeGuideConfig.class);
    }
}
