package com.lumbridgeguide;

import com.google.inject.Provides;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.TeamChatPrefixService;
import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.ui.SidebarController;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;

import javax.inject.Inject;
import java.util.List;

@Slf4j
@PluginDescriptor(
        name = "Lumbridge Guide"
)
@PluginDependency(BankTagsPlugin.class)
public class LumbridgeGuidePlugin extends Plugin {

    @Inject
    private EventBus eventBus;

    @Inject
    private SidebarController sidebarController;

    @Inject
    private AccountSyncService accountSyncService;

    @Inject
    private TileProgressTracker tileProgressTracker;

    @Inject
    private ProofService proofService;

    @Inject
    private TeamChatPrefixService teamChatPrefixService;

    @Override
    protected void startUp() {
        log.debug("Lumbridge Guide started");
        accountSyncService.startUp();
        proofService.startUp();
        eventSubscribers().forEach(eventBus::register);
        sidebarController.startUp();
    }

    @Override
    protected void shutDown() {
        log.debug("Lumbridge Guide stopped");
        eventSubscribers().forEach(eventBus::unregister);
        sidebarController.shutDown();
        accountSyncService.reset();
    }

    private List<Object> eventSubscribers() {
        return List.of(sidebarController, accountSyncService, tileProgressTracker, proofService,
                teamChatPrefixService);
    }

    @Provides
    public LumbridgeGuideConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LumbridgeGuideConfig.class);
    }
}
