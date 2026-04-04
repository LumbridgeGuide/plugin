package com.lumbridgeguide;

import com.google.inject.Provides;
import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTeamData;
import com.lumbridgeguide.service.BoardDataService;
import com.lumbridgeguide.ui.LumbridgeGuidePanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
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
    private LumbridgeGuideConfig config;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private BoardDataService boardDataService;

    private LumbridgeGuidePanel panel;
    private NavigationButton navigationButton;

    @Override
    protected void startUp() throws Exception {
        log.info("Lumbridge Guide started");

        boardDataService.refresh();

        panel = new LumbridgeGuidePanel(boardDataService, config);

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
        updateChatboxInputPrefix();
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
        if (LumbridgeGuideConfig.CONFIG_GROUP.equals(event.getGroup()) && panel != null) {
            boardDataService.refresh();
            panel.refresh();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
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
        if (team == null || team.getName() == null || team.getColor() == null) {
            return;
        }

        String colorHex = team.getColor().replace("#", "");
        String prefix = "<col=" + colorHex + ">[" + team.getName() + "]</col> ";

        MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(prefix + messageNode.getName());
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
        if (team == null || team.getName() == null || team.getColor() == null) {
            return;
        }

        String playerName = client.getLocalPlayer().getName();
        String currentText = chatboxInput.getText();
        String teamTag = "[" + team.getName() + "]";

        if (currentText != null && currentText.contains(playerName) && !currentText.contains(teamTag)) {
            String colorHex = team.getColor().replace("#", "");
            String prefix = "<col=" + colorHex + ">" + teamTag + "</col> ";
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
