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
import lombok.Setter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.SkillIconManager;
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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@Slf4j
@Singleton
public class ProofService {

    /** A screenshot ready to send as proof for one tile. */
    @Value
    public static class Offer {
        String id;
        PluginBoardData board;
        PluginTileData tile;
        String reason;
        BufferedImage screenshot;
        TileProgressTracker.XpReading xp;
        long accountHash;
    }

    private static final int PRUNE_EVERY_TICKS = 100;
    private static final DateTimeFormatter STAMP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color STAMP_BACKGROUND = new Color(0, 0, 0, 190);
    private static final Color STAMP_ACCENT = new Color(0xE07A3A);
    private static final Color STAMP_TEXT = new Color(0xE4E4E7);

    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;
    private final LumbridgeGuideClient apiClient;
    private final DrawManager drawManager;
    private final ItemManager itemManager;
    private final ChatMessageManager chatMessageManager;
    private final TileProgressTracker progressTracker;
    private final SkillIconManager skillIconManager;
    private final Client client;
    private final Gson gson;

    private final ProofOfferStore store;
    private final TileCompletePopup popup;
    private final ScheduledExecutorService executor;

    private final Set<String> offeredTiles = ConcurrentHashMap.newKeySet();
    private final List<Offer> queue = new CopyOnWriteArrayList<>();
    private boolean restored;
    private int ticksSincePrune;

    /** The offers waiting to be sent, oldest first. Called on any thread whenever the queue changes. */
    @Setter
    private Consumer<List<Offer>> onQueueChanged = offers -> { };

    @Inject
    public ProofService(LumbridgeGuideConfig config, BoardDataService boardDataService,
                        LumbridgeGuideClient apiClient, DrawManager drawManager, ItemManager itemManager,
                        ChatMessageManager chatMessageManager, TileProgressTracker progressTracker,
                        SkillIconManager skillIconManager, Client client, ProofOfferStore store,
                        TileCompletePopup popup, ScheduledExecutorService executor, Gson gson) {
        this.config = config;
        this.boardDataService = boardDataService;
        this.apiClient = apiClient;
        this.drawManager = drawManager;
        this.itemManager = itemManager;
        this.chatMessageManager = chatMessageManager;
        this.progressTracker = progressTracker;
        this.skillIconManager = skillIconManager;
        this.client = client;
        this.store = store;
        this.popup = popup;
        this.executor = executor;
        this.gson = gson;
        progressTracker.setOnTargetReached((board, tile) -> offer(board, tile,
                "kill_count".equals(tile.getType()) ? "Kill count reached" : "XP target reached"));
    }

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

    public void captureManually(PluginBoardData board, PluginTileData tile) {
        capture(board, tile, "Proof for this tile");
    }

    public List<Offer> getQueue() {
        return List.copyOf(queue);
    }

    /** Drops an offer the player doesn't want to send, here and on disk. */
    public void dismiss(Offer offer) {
        remove(offer);
    }

    /**
     * Offers saved before the client last closed come back once the player is in game and the boards have loaded.
     * Offers whose board has ended, or whose tile the team has claimed some other way, are dropped as they're found.
     */
    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!restored && boardDataService.hasCachedData()) {
            restored = true;
            long accountHash = client.getAccountHash();
            executor.execute(() -> restore(accountHash));
        }
        if (++ticksSincePrune >= PRUNE_EVERY_TICKS) {
            ticksSincePrune = 0;
            rearmRejected();
            queue.stream().filter(offer -> current(offer.getBoard().getId(), offer.getTile().getId()).isEmpty())
                    .forEach(this::remove);
        }
    }

    /** The queue belongs to the account that was logged in, so it is cleared on logout and restored on login. */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            restored = false;
            offeredTiles.clear();
            queue.clear();
            onQueueChanged.accept(List.of());
        }
    }

    /**
     * Proof the owner turned down frees the tile up again: completing it again, such as another drop or kill, offers
     * fresh proof. Proof already waiting in the queue keeps the tile from being offered twice.
     */
    private void rearmRejected() {
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : board.getTiles() == null ? List.<PluginTileData>of() : board.getTiles()) {
                String key = board.getId() + tile.getId();
                boolean queued = queue.stream().anyMatch(offer -> offer.getTile().getId().equals(tile.getId()));
                if (tile.isProofRejected() && !queued && offeredTiles.remove(key)) {
                    progressTracker.rearm(tile.getId());
                }
            }
        }
    }

    private void restore(long accountHash) {
        for (ProofOfferStore.Loaded loaded : store.load(accountHash)) {
            ProofOfferStore.Saved saved = loaded.getSaved();
            Optional<Offer> offer = current(saved.getBoardId(), saved.getTileId()).map(boardAndTile -> new Offer(
                    saved.getId(),
                    boardAndTile.getKey(),
                    boardAndTile.getValue(),
                    saved.getReason(),
                    loaded.getScreenshot(),
                    xpOf(saved),
                    saved.getAccountHash()));
            if (offer.isEmpty() || queue.stream().anyMatch(queued -> queued.getId().equals(saved.getId()))) {
                if (offer.isEmpty()) {
                    store.delete(accountHash, saved.getId());
                }
                continue;
            }
            offeredTiles.add(saved.getBoardId() + saved.getTileId());
            queue.add(offer.get());
        }
        onQueueChanged.accept(getQueue());
    }

    private static TileProgressTracker.XpReading xpOf(ProofOfferStore.Saved saved) {
        if (saved.getXpSkill() == null || saved.getXpTotal() == null) {
            return null;
        }
        try {
            return new TileProgressTracker.XpReading(
                    Skill.valueOf(saved.getXpSkill()), saved.getXpGained(), saved.getXpTotal());
        } catch (IllegalArgumentException unknownSkill) {
            return null;
        }
    }

    /** The board and tile as the latest sync has them, only while the board runs and the team still needs the tile. */
    private Optional<Map.Entry<PluginBoardData, PluginTileData>> current(String boardId, String tileId) {
        return boardDataService.getRunningBoards().stream()
                .filter(board -> board.getId().equals(boardId))
                .findFirst()
                .flatMap(board -> (board.getTiles() == null ? List.<PluginTileData>of() : board.getTiles()).stream()
                        .filter(tile -> tile.getId().equals(tileId) && isOpenForMyTeam(board, tile))
                        .findFirst()
                        .map(tile -> Map.entry(board, tile)));
    }

    private void enqueue(Offer offer) {
        queue.add(offer);
        while (queue.size() > ProofOfferStore.MAX_SAVED) {
            queue.remove(0);
        }
        onQueueChanged.accept(getQueue());
    }

    private void remove(Offer offer) {
        if (queue.removeIf(queued -> queued.getId().equals(offer.getId()))) {
            onQueueChanged.accept(getQueue());
        }
        executor.execute(() -> store.delete(offer.getAccountHash(), offer.getId()));
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
        Map<String, String> fields = new HashMap<>();
        fields.put("tileId", offer.getTile().getId());
        fields.put("verificationCode", offer.getBoard().getVerificationCode());
        if (offer.getXp() != null) {
            fields.put("xpAtSubmit", String.valueOf(offer.getXp().getTotal()));
            if (offer.getXp().getGained() != null) {
                fields.put("xpGained", String.valueOf(offer.getXp().getGained()));
            }
            if (offer.getAccountHash() != -1) {
                fields.put("accountHash", String.valueOf(offer.getAccountHash()));
            }
        }
        apiClient.postImage("/plugin/bingo/" + offer.getBoard().getId() + "/submissions", fields, "image", png,
                response -> {
                    SubmissionResultData result = gson.fromJson(response.getBody(), SubmissionResultData.class);
                    boardDataService.refresh();
                    onDone.accept(result == null ? "Sent" : result.getMessage());
                    remove(offer);
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
        if (!config.offerProof() || !isOpenForMyTeam(board, tile) || tile.isProofPending()
                || !offeredTiles.add(board.getId() + tile.getId())) {
            return;
        }
        popup.show("Bingo tile complete!", tile.getTitle() + "<br>" + board.getTitle(),
                () -> capture(board, tile, reason));
        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.CONSOLE)
                .runeLiteFormattedMessage("Lumbridge Guide: " + tile.getTitle()
                        + " matches a tile. Open the panel to send proof.")
                .build());
    }

    /** Only a frame of the game itself is proof, never the login or welcome screen. */
    private void capture(PluginBoardData board, PluginTileData tile, String reason) {
        if (board.getVerificationCode() == null || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        TileProgressTracker.XpReading xp = "skill_xp".equals(tile.getType())
                ? progressTracker.xpReading(board, tile).orElse(null)
                : null;
        long accountHash = client.getAccountHash();
        BufferedImage icon = xp == null ? null : skillIconManager.getSkillImage(xp.getSkill());
        drawManager.requestNextFrameListener(frame -> {
            BufferedImage screenshot = stamp(frame, board.getVerificationCode());
            if (xp != null) {
                drawXpCard(screenshot, xp, tile.getXpTarget(), icon);
            }
            Offer offer = new Offer(UUID.randomUUID().toString(), board, tile, reason, screenshot, xp, accountHash);
            enqueue(offer);
            executor.execute(() -> store.save(new ProofOfferStore.Saved(
                    offer.getId(),
                    board.getId(),
                    tile.getId(),
                    reason,
                    xp == null ? null : xp.getSkill().name(),
                    xp == null ? null : xp.getGained(),
                    xp == null ? null : xp.getTotal(),
                    accountHash,
                    System.currentTimeMillis()), screenshot));
        });
    }

    /**
     * Draws the XP behind XP proof in the top left, in the same style as the code stamp, since nothing else on a
     * screenshot shows it. It stays clear of the chatbox, where level-up messages back it up. The website checks these
     * numbers against the OSRS hiscores.
     */
    static void drawXpCard(BufferedImage image, TileProgressTracker.XpReading xp, long target, BufferedImage icon) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.UK);
        String title = xp.getSkill().getName();
        String gained = xp.getGained() == null
                ? "XP gain not tracked"
                : "+" + numbers.format(xp.getGained()) + " XP since the event started";
        String totals = (target > 0 ? "Target " + numbers.format(target) + "  ·  " : "")
                + "Total " + numbers.format(xp.getTotal()) + " XP";

        Graphics2D canvas = image.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        FontMetrics titleMetrics = canvas.getFontMetrics(titleFont);
        FontMetrics bodyMetrics = canvas.getFontMetrics(bodyFont);
        int iconSize = icon == null ? 0 : titleMetrics.getAscent() + 4;
        int padding = 10;
        int width = padding * 2 + Math.max(
                iconSize + (icon == null ? 0 : 8) + titleMetrics.stringWidth(title),
                Math.max(bodyMetrics.stringWidth(gained), bodyMetrics.stringWidth(totals)));
        int height = padding * 2 + Math.max(iconSize, titleMetrics.getHeight()) + 4 + bodyMetrics.getHeight() * 2;
        int x = 10;
        int y = 10;

        canvas.setColor(STAMP_BACKGROUND);
        canvas.fillRect(x, y, width, height);
        int lineTop = y + padding;
        int textX = x + padding;
        if (icon != null) {
            canvas.drawImage(icon, textX, lineTop, iconSize, iconSize, null);
            textX += iconSize + 8;
        }
        canvas.setFont(titleFont);
        canvas.setColor(STAMP_ACCENT);
        canvas.drawString(title, textX, lineTop + (Math.max(iconSize, titleMetrics.getHeight()) + titleMetrics.getAscent()
                - titleMetrics.getDescent()) / 2);
        canvas.setFont(bodyFont);
        canvas.setColor(STAMP_TEXT);
        int bodyTop = lineTop + Math.max(iconSize, titleMetrics.getHeight()) + 4;
        canvas.drawString(gained, x + padding, bodyTop + bodyMetrics.getAscent());
        canvas.drawString(totals, x + padding, bodyTop + bodyMetrics.getHeight() + bodyMetrics.getAscent());
        canvas.dispose();
    }

    /** Copies the frame and stamps the board code and the time in the bottom right, where the website expects it. */
    static BufferedImage stamp(Image frame, String code) {
        BufferedImage image = new BufferedImage(frame.getWidth(null), frame.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = image.createGraphics();
        canvas.drawImage(frame, 0, 0, null);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        String text = code + "  " + LocalDateTime.now().format(STAMP_TIME);
        FontMetrics metrics = canvas.getFontMetrics();
        int width = metrics.stringWidth(text) + 20;
        int height = metrics.getHeight() + 10;
        int x = image.getWidth() - width - 10;
        int y = image.getHeight() - height - 10;
        canvas.setColor(STAMP_BACKGROUND);
        canvas.fillRect(x, y, width, height);
        canvas.setColor(STAMP_ACCENT);
        canvas.drawString(text, x + 10, y + 5 + metrics.getAscent());
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
