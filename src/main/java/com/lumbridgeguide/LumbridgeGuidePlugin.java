package com.lumbridgeguide;

import com.google.inject.Provides;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.ProofService;
import com.lumbridgeguide.bingo.TeamChatPrefix;
import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import com.lumbridgeguide.notifications.InboxService;
import com.lumbridgeguide.ui.SidebarPanel;
import com.lumbridgeguide.ui.Theme;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.SwingUtilities;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@PluginDescriptor(
        name = "Lumbridge Guide"
)
@PluginDependency(BankTagsPlugin.class)
public class LumbridgeGuidePlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private LumbridgeGuideConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BoardDataService boardDataService;

    @Inject
    private GearTagService gearTagService;

    @Inject
    private AccountSyncService accountSyncService;

    @Inject
    private GearConfigExportService gearConfigExportService;

    @Inject
    private TripCheckService tripCheckService;

    @Inject
    private TileProgressTracker tileProgressTracker;

    @Inject
    private ProofService proofService;

    @Inject
    private InboxService inboxService;

    @Inject
    private ItemManager itemManager;

    @Inject
    private SkillIconManager skillIconManager;

    private static final Set<GameState> LOGGED_OUT_STATES = EnumSet.of(
            GameState.UNKNOWN,
            GameState.STARTING,
            GameState.LOGIN_SCREEN,
            GameState.LOGIN_SCREEN_AUTHENTICATOR,
            GameState.LOGGING_IN);

    private SidebarPanel panel;
    private NavigationButton navigationButton;
    private boolean awaitingLogin = true;
    private boolean loginPending;
    private Instant lastBoardRefresh = Instant.now();

    @Override
    protected void startUp() throws Exception {
        log.info("Lumbridge Guide started");

        boardDataService.refresh();
        lastBoardRefresh = Instant.now();

        awaitingLogin = true;
        loginPending = client.getGameState() == GameState.LOGGED_IN;

        Theme.setAccent(config.accentColour());
        addPanel();
    }

    private void addPanel() {
        panel = new SidebarPanel(boardDataService, tileProgressTracker, proofService, gearTagService,
                gearConfigExportService,
                tripCheckService, accountSyncService, inboxService, itemManager, skillIconManager, config);

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
    public void onGameTick(GameTick tick) {
        if (loginPending) {
            loginPending = false;
            accountSyncService.onLoggedIn();
            tileProgressTracker.onStatsChanged();
        }
        tileProgressTracker.reportIfDue();
        inboxService.pollIfDue();
        updateChatboxInputPrefix();
        refreshBoardsWhenDue();
    }

    private void refreshBoardsWhenDue() {
        int minutes = config.boardRefreshMinutes();
        if (minutes <= 0 || Duration.between(lastBoardRefresh, Instant.now()).toMinutes() < minutes) {
            return;
        }
        lastBoardRefresh = Instant.now();
        boardDataService.refresh(() -> {
            if (panel != null) {
                panel.refresh();
            }
        });
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        GameState state = event.getGameState();
        if (LOGGED_OUT_STATES.contains(state)) {
            if (panel != null) {
                panel.setBankOpen(false);
            }
            if (!awaitingLogin) {
                loginPending = false;
                accountSyncService.onLoggedOut();
                tileProgressTracker.reset();
            }
            awaitingLogin = true;
        } else if (state == GameState.LOGGED_IN && awaitingLogin) {
            awaitingLogin = false;
            loginPending = true;
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

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        proofService.onLoot(event.getItems());
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        accountSyncService.onStatChanged();
        tileProgressTracker.onStatsChanged();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (!LumbridgeGuideConfig.CONFIG_GROUP.equals(event.getGroup()) || panel == null) {
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
        boardDataService.refresh();
        panel.refresh();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() == VarPlayerID.QP) {
            accountSyncService.onQuestPointsChanged();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE) {
            tileProgressTracker.onGameMessage(chatMessage.getMessage());
            return;
        }
        if (!config.showTeamPrefix()) {
            return;
        }

        ChatMessageType type = chatMessage.getType();
        if (type != ChatMessageType.PUBLICCHAT
                && type != ChatMessageType.MODCHAT
                && type != ChatMessageType.FRIENDSCHAT
                && type != ChatMessageType.CLAN_CHAT
                && type != ChatMessageType.CLAN_GUEST_CHAT) {
            return;
        }

        if (client.getLocalPlayer() == null) {
            return;
        }

        String localName = client.getLocalPlayer().getName();
        if (localName == null || !localName.equals(chatMessage.getName())) {
            return;
        }

        PluginTeamData team = resolveActiveTeam();
        if (team == null || team.getName() == null) {
            return;
        }

        MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(TeamChatPrefix.of(team.getName(), team.getColor()) + messageNode.getName());
    }

    private void updateChatboxInputPrefix() {
        if (!config.showTeamPrefix()) {
            return;
        }

        Widget chatboxInput = client.getWidget(ComponentID.CHATBOX_INPUT);
        if (chatboxInput == null) {
            return;
        }

        if (client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null) {
            return;
        }

        PluginTeamData team = resolveActiveTeam();
        if (team == null || team.getName() == null) {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        String currentText = chatboxInput.getText();
        String prefix = TeamChatPrefix.of(team.getName(), team.getColor());

        if (currentText != null && currentText.contains(playerName) && !currentText.contains(prefix)) {
            chatboxInput.setText(currentText.replace(playerName + ":", prefix + playerName + ":"));
        }
    }

    private PluginTeamData resolveActiveTeam() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        if (boards.isEmpty()) {
            return null;
        }
        return boards.get(0).getMyTeam();
    }

    @Provides
    LumbridgeGuideConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(LumbridgeGuideConfig.class);
    }
}
