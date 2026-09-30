package com.lumbridgeguide.bingo;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Puts the player's bingo team name, in the team colour, in front of their name in chat and in the chat input. */
@Singleton
public class TeamChatPrefixService {

    private static final Set<ChatMessageType> PREFIXED_TYPES = EnumSet.of(
            ChatMessageType.PUBLICCHAT,
            ChatMessageType.MODCHAT,
            ChatMessageType.FRIENDSCHAT,
            ChatMessageType.CLAN_CHAT,
            ChatMessageType.CLAN_GUEST_CHAT);

    private final Client client;
    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;

    @Inject
    public TeamChatPrefixService(Client client, LumbridgeGuideConfig config, BoardDataService boardDataService) {
        this.client = client;
        this.config = config;
        this.boardDataService = boardDataService;
    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (!config.showTeamPrefix() || !PREFIXED_TYPES.contains(chatMessage.getType())
                || client.getLocalPlayer() == null) {
            return;
        }
        String localName = client.getLocalPlayer().getName();
        if (localName == null || !localName.equals(chatMessage.getName())) {
            return;
        }
        PluginTeamData team = activeTeam();
        if (team == null || team.getName() == null) {
            return;
        }
        MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(TeamChatPrefix.of(team.getName(), team.getColor()) + messageNode.getName());
    }

    /** The chat input is redrawn by the game, so the prefix is put back every tick. */
    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!config.showTeamPrefix()) {
            return;
        }
        Widget chatboxInput = client.getWidget(ComponentID.CHATBOX_INPUT);
        if (chatboxInput == null || client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null) {
            return;
        }
        PluginTeamData team = activeTeam();
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

    private PluginTeamData activeTeam() {
        List<PluginBoardData> boards = boardDataService.getBoards();
        return boards.isEmpty() ? null : boards.get(0).getMyTeam();
    }
}
