package com.lumbridgeguide.bingo;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.bingo.data.BoardEventType;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginBoardEvent;
import com.lumbridgeguide.bingo.data.PluginClaimEvent;
import com.lumbridgeguide.bingo.data.PluginPollResponse;
import com.lumbridgeguide.bingo.data.PluginProofResult;
import com.lumbridgeguide.bingo.data.ProofStatus;
import com.lumbridgeguide.ui.Theme;
import lombok.RequiredArgsConstructor;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BingoChatAnnouncer {

    private final ChatMessageManager chatMessageManager;
    private final LumbridgeGuideConfig config;

    public void announce(PluginPollResponse poll, boolean firstPoll, List<PluginBoardData> boards) {
        if (!firstPoll && poll.getClaims() != null) {
            List<PluginClaimEvent> oldestFirst = new ArrayList<>(poll.getClaims());
            Collections.reverse(oldestFirst);
            oldestFirst.stream().filter(claim -> shouldAnnounce(claim, boards)).forEach(claim -> send(claim.getBoardTitle(),
                    escape(claim.getTeamName()) + " claimed " + escape(claim.getTileTitle())));
        }

        if (poll.getResults() != null) {
            poll.getResults().forEach(result -> send(result.getBoardTitle(), resultText(result)));
        }

        if (poll.getBoardEvents() != null) {
            poll.getBoardEvents().forEach(event -> send(event.getBoardTitle(), boardEventText(event)));
        }
    }

    private boolean shouldAnnounce(PluginClaimEvent claim, List<PluginBoardData> boards) {
        switch (config.claimMessages()) {
            case ALL_TEAMS:
                return true;
            case MY_TEAM:
                return boards.stream().anyMatch(board -> board.getId().equals(claim.getBoardId())
                        && board.getMyTeam() != null && claim.getTeamId() != null
                        && claim.getTeamId().equals(board.getMyTeam().getId()));
            default:
                return false;
        }
    }

    private static String resultText(PluginProofResult result) {
        String decision = result.getStatus() == ProofStatus.REJECTED ? "turned down" : "accepted";
        String tile = escape(result.getTileTitle());
        return result.isReversed()
                ? "The decision on your proof for " + tile + " was reversed. It's now " + decision
                : "Your proof for " + tile + " was " + decision;
    }

    private static String boardEventText(PluginBoardEvent event) {
        return event.getType() == BoardEventType.ENDED ? "The board has ended" : "The board has started";
    }

    private void send(String boardTitle, String text) {
        String message = "[" + escape(boardTitle) + "] " + text;
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage(ColorUtil.wrapWithColorTag(message, Theme.accent()))
                .build());
    }

    private static String escape(String text) {
        return text == null ? "" : Text.escapeJagex(text);
    }
}
