package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.ApiErrorData;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.SubmissionResultData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.ui.DrawManager;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Offers to send proof when the game shows a player completing an open tile: a drop of one of its items, or tracked
 * kill count or XP reaching its target. The offer carries a screenshot of the next frame with the board's verification
 * code and the time stamped on it, and sending it uploads the screenshot as a claim submission.
 */
@Slf4j
@Singleton
public class ProofService {

    /** A screenshot ready to send as proof for one tile. */
    @Value
    public static class Offer {
        PluginBoardData board;
        PluginTileData tile;
        String reason;
        BufferedImage screenshot;
    }

    private static final DateTimeFormatter STAMP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;
    private final LumbridgeGuideClient apiClient;
    private final DrawManager drawManager;
    private final ItemManager itemManager;
    private final ChatMessageManager chatMessageManager;
    private final Gson gson;

    private final Set<String> offeredTiles = new HashSet<>();
    private Consumer<Offer> onOffer = offer -> { };

    @Inject
    public ProofService(LumbridgeGuideConfig config, BoardDataService boardDataService,
                        LumbridgeGuideClient apiClient, DrawManager drawManager, ItemManager itemManager,
                        ChatMessageManager chatMessageManager, TileProgressTracker progressTracker, Gson gson) {
        this.config = config;
        this.boardDataService = boardDataService;
        this.apiClient = apiClient;
        this.drawManager = drawManager;
        this.itemManager = itemManager;
        this.chatMessageManager = chatMessageManager;
        this.gson = gson;
        progressTracker.setOnTargetReached((board, tile) -> offer(board, tile,
                "kill_count".equals(tile.getType()) ? "Kill count reached" : "XP target reached"));
    }

    /** The panel shows each offer as it arrives. Called with the offer on the render thread. */
    public void setOnOffer(Consumer<Offer> listener) {
        onOffer = listener;
    }

    /** Checks each batch of NPC loot against the open item drop tiles. */
    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        Set<Integer> dropped = new HashSet<>();
        for (ItemStack item : event.getItems()) {
            dropped.add(ItemVariationMapping.map(itemManager.canonicalize(item.getId())));
        }
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : board.getTiles() == null ? List.<PluginTileData>of() : board.getTiles()) {
                if ("item_drop".equals(tile.getType()) && matches(tile, dropped)) {
                    offer(board, tile, "Drop matched a tile");
                }
            }
        }
    }

    /** Takes a screenshot now to send as proof for any tile, from the tile's details. */
    public void captureManually(PluginBoardData board, PluginTileData tile) {
        capture(board, tile, "Proof for this tile");
    }

    public void submit(Offer offer, Consumer<String> onDone) {
        byte[] png;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(offer.getScreenshot(), "png", buffer);
            png = buffer.toByteArray();
        } catch (IOException exception) {
            onDone.accept("Couldn't save the screenshot");
            return;
        }
        Map<String, String> fields = Map.of(
                "tileId", offer.getTile().getId(),
                "verificationCode", offer.getBoard().getVerificationCode());
        apiClient.postImage("/plugin/bingo/" + offer.getBoard().getId() + "/submissions", fields, "image", png,
                response -> {
                    SubmissionResultData result = gson.fromJson(response.getBody(), SubmissionResultData.class);
                    if (result != null && result.isClaimed()) {
                        boardDataService.refresh();
                    }
                    onDone.accept(result == null ? "Sent" : result.getMessage());
                },
                response -> onDone.accept(failureMessage(response)));
    }

    private String failureMessage(ApiResponse response) {
        switch (response.getStatusCode()) {
            case 400:
            case 409:
                ApiErrorData error = parseError(response);
                return error != null && error.getMessage() != null
                        ? error.getMessage() : "The website turned this proof down";
            case 401:
                return "Check your API key in the plugin settings";
            case 403:
                return "You need to be on a team on this board";
            case -1:
                return "Could not reach Lumbridge Guide";
            default:
                return "Sending failed (" + response.getStatusCode() + ")";
        }
    }

    private ApiErrorData parseError(ApiResponse response) {
        try {
            return gson.fromJson(response.getBody(), ApiErrorData.class);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void offer(PluginBoardData board, PluginTileData tile, String reason) {
        if (!config.offerProof() || !isOpenForMyTeam(board, tile) || !offeredTiles.add(board.getId() + tile.getId())) {
            return;
        }
        capture(board, tile, reason);
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage("Lumbridge Guide: " + tile.getTitle()
                        + " matches a tile. Open the panel to send proof.")
                .build());
    }

    private void capture(PluginBoardData board, PluginTileData tile, String reason) {
        if (board.getVerificationCode() == null) {
            return;
        }
        drawManager.requestNextFrameListener(frame ->
                onOffer.accept(new Offer(board, tile, reason, stamp(frame, board.getVerificationCode()))));
    }

    /** Copies the frame and stamps the board code and the time in the bottom right, where the website expects it. */
    static BufferedImage stamp(Image frame, String code) {
        BufferedImage image = new BufferedImage(frame.getWidth(null), frame.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = image.createGraphics();
        canvas.drawImage(frame, 0, 0, null);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        String text = code + "  " + LocalDateTime.now().format(STAMP_TIME);
        FontMetrics metrics = canvas.getFontMetrics();
        int width = metrics.stringWidth(text) + 16;
        int height = metrics.getHeight() + 8;
        int x = image.getWidth() - width - 8;
        int y = image.getHeight() - height - 8;
        canvas.setColor(new Color(0, 0, 0, 190));
        canvas.fillRect(x, y, width, height);
        canvas.setColor(new Color(0xE07A3A));
        canvas.drawString(text, x + 8, y + 4 + metrics.getAscent());
        canvas.dispose();
        return image;
    }

    private boolean matches(PluginTileData tile, Set<Integer> dropped) {
        if (tile.getItems() == null) {
            return false;
        }
        for (TileItemEntry item : tile.getItems()) {
            if (dropped.contains(ItemVariationMapping.map(item.getId()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpenForMyTeam(PluginBoardData board, PluginTileData tile) {
        PluginTeamData team = board.getMyTeam();
        return team != null && !(tile.isClaimed() && team.getId().equals(tile.getClaimedByTeamId()));
    }
}
