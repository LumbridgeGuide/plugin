package com.lumbridgeguide.bingo;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.EnumSet;
import java.util.Set;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
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

    private String shownPrefix;

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (!config.showTeamPrefix() || !PREFIXED_TYPES.contains(chatMessage.getType())
                || client.getLocalPlayer() == null
                || !isOwnName(client.getLocalPlayer().getName(), chatMessage.getName())) {
            return;
        }

        String prefix = ownTeamPrefix();
        if (prefix == null) {
            return;
        }

        MessageNode messageNode = chatMessage.getMessageNode();
        messageNode.setName(prefix + messageNode.getName());
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        Widget chatboxInput = client.getWidget(InterfaceID.Chatbox.INPUT);
        if (!config.showTeamPrefix() || chatboxInput == null || chatboxInput.getText() == null
                || client.getLocalPlayer() == null || client.getLocalPlayer().getName() == null) {
            return;
        }

        String currentText = chatboxInput.getText();
        String prefix = ownTeamPrefix();
        String updatedText = withPrefix(currentText, client.getLocalPlayer().getName(), shownPrefix, prefix);
        shownPrefix = prefix;
        if (!updatedText.equals(currentText)) {
            chatboxInput.setText(updatedText);
        }
    }

    private static boolean isOwnName(String localName, String chatName) {
        return localName != null && chatName != null && Text.sanitize(localName).equals(Text.sanitize(chatName));
    }

    private static String withPrefix(String text, String playerName, String oldPrefix, String newPrefix) {
        String label = playerName + ":";
        String plain = oldPrefix == null ? text : text.replace(oldPrefix + label, label);
        return newPrefix == null ? plain : plain.replace(label, newPrefix + label);
    }

    private String ownTeamPrefix() {
        PluginBoardData board = boardDataService.getSelectedBoard();
        PluginTeamData team = board == null ? null : board.getMyTeam();
        return team == null || team.getName() == null ? null : TeamChatPrefix.of(team.getName(), team.getColor());
    }
}
