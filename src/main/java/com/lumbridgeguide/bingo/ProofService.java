package com.lumbridgeguide.bingo;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.SubmissionResultData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import com.lumbridgeguide.bingo.data.TileType;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStack;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.Text;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class ProofService {

    private static final int PRUNE_EVERY_TICKS = 100;

    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;
    private final LumbridgeGuideClient apiClient;
    private final DrawManager drawManager;
    private final ItemManager itemManager;
    private final ChatMessageManager chatMessageManager;
    private final TileProgressTracker progressTracker;
    private final SkillIconManager skillIconManager;
    private final Client client;
    private final ClientThread clientThread;
    private final ProofOfferStore store;
    private final TileCompletePopup popup;
    private final ScheduledExecutorService executor;

    private final Set<String> offeredTiles = ConcurrentHashMap.newKeySet();
    private final List<ProofOffer> queue = new CopyOnWriteArrayList<>();
    private boolean restored;
    private int ticksSincePrune;

    @Setter
    private Consumer<List<ProofOffer>> onQueueChanged = offers -> { };

    public void startUp() {
        progressTracker.setOnTargetReached((boardAndTile, detection) -> offer(boardAndTile.getKey(),
                boardAndTile.getValue(), boardAndTile.getValue().getType() == TileType.KILL_COUNT
                        ? "Kill count reached" : "XP target reached", detection));
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        Set<Integer> dropped = new HashSet<>();
        for (ItemStack item : event.getItems()) {
            dropped.add(ItemVariationMapping.map(itemManager.canonicalize(item.getId())));
        }
        NPC npc = event.getNpc();
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : board.tiles()) {
                Integer itemId = matchedItem(tile, dropped);
                if (tile.getType() == TileType.ITEM_DROP && itemId != null) {
                    offer(board, tile, "Drop received",
                            Detection.drop(itemId, npc == null ? null : npc.getId(), npc == null ? null : npc.getName()));
                }
            }
        }
    }

    public void captureManually(PluginBoardData board, PluginTileData tile) {
        clientThread.invoke(() -> capture(board, tile, "Proof for this tile", null));
    }

    public List<ProofOffer> getQueue() {
        return List.copyOf(queue);
    }

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
            queue.stream().filter(this::isEnded).forEach(this::remove);
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            restored = false;
            offeredTiles.clear();
            queue.clear();
            onQueueChanged.accept(List.of());
        }
    }

    public boolean isEnded(ProofOffer offer) {
        return boardDataService.hasCachedData()
                && current(offer.getBoard().getId(), offer.getTile().getId()).isEmpty();
    }

    private void rearmRejected() {
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : board.tiles()) {
                boolean queued = queue.stream().anyMatch(offer -> offer.getTile().getId().equals(tile.getId()));
                if (tile.isProofRejected() && !queued && offeredTiles.remove(key(board.getId(), tile.getId()))) {
                    progressTracker.rearm(tile.getId());
                }
            }
        }
    }

    private void restore(long accountHash) {
        for (LoadedProof loaded : store.load(accountHash)) {
            SavedProof saved = loaded.getSaved();
            Optional<ProofOffer> offer = current(saved.getBoardId(), saved.getTileId())
                    .map(boardAndTile -> new ProofOffer(
                            saved.getId(),
                            boardAndTile.getKey(),
                            boardAndTile.getValue(),
                            saved.getReason(),
                            loaded.getScreenshot(),
                            loaded.getPng(),
                            saved.getImageSha256(),
                            xpOf(saved),
                            saved.getAccountHash(),
                            saved.getDetection()));
            if (offer.isEmpty() || queue.stream().anyMatch(queued -> queued.getId().equals(saved.getId()))) {
                if (offer.isEmpty()) {
                    store.delete(accountHash, saved.getId());
                }
                continue;
            }

            offeredTiles.add(key(saved.getBoardId(), saved.getTileId()));
            queue.add(offer.get());
        }
        onQueueChanged.accept(getQueue());
    }

    private static XpReading xpOf(SavedProof saved) {
        if (saved.getXpSkill() == null || saved.getXpTotal() == null) {
            return null;
        }

        try {
            return new XpReading(Skill.valueOf(saved.getXpSkill()), saved.getXpGained(), saved.getXpTotal());
        } catch (IllegalArgumentException unknownSkill) {
            return null;
        }
    }

    private Optional<Map.Entry<PluginBoardData, PluginTileData>> current(String boardId, String tileId) {
        return boardDataService.getRunningBoards().stream()
                .filter(board -> board.getId().equals(boardId))
                .findFirst()
                .flatMap(board -> board.tiles().stream()
                        .filter(tile -> tile.getId().equals(tileId) && isOpenForMyTeam(board, tile))
                        .findFirst()
                        .map(tile -> Map.entry(board, tile)));
    }

    private void enqueue(ProofOffer offer) {
        queue.add(offer);
        while (queue.size() > ProofOfferStore.MAX_SAVED) {
            queue.remove(0);
        }
        onQueueChanged.accept(getQueue());
    }

    public void remove(ProofOffer offer) {
        if (queue.removeIf(queued -> queued.getId().equals(offer.getId()))) {
            onQueueChanged.accept(getQueue());
        }
        executor.execute(() -> store.delete(offer.getAccountHash(), offer.getId()));
    }

    public void submit(ProofOffer offer, Consumer<String> onDone) {
        executor.execute(() -> send(offer, onDone));
    }

    private void send(ProofOffer offer, Consumer<String> onDone) {
        Map<String, String> fields = new HashMap<>();
        fields.put("tileId", offer.getTile().getId());
        fields.put("verificationCode", offer.getBoard().getVerificationCode());
        if (offer.getXp() != null) {
            fields.put("xpAtSubmit", String.valueOf(offer.getXp().getTotal()));
            fields.put("xpGained", Objects.toString(offer.getXp().getGained(), null));
            if (config.syncOnLoginLogout() && offer.getAccountHash() != AccountSyncService.UNKNOWN_ACCOUNT_HASH) {
                fields.put("accountHash", String.valueOf(offer.getAccountHash()));
            }
        }
        Detection detection = offer.getDetection();
        fields.put("source", (detection == null ? ProofSource.MANUAL : ProofSource.DETECTED).name());
        if (detection != null) {
            fields.put("detectedEvent", detection.getEvent().name());
            fields.put("detectedItemId", Objects.toString(detection.getItemId(), null));
            fields.put("detectedNpcId", Objects.toString(detection.getNpcId(), null));
            fields.put("detectedNpcName", detection.getNpcName());
            fields.put("detectedKillCount", Objects.toString(detection.getKillCount(), null));
            fields.put("detectedAt", detection.getDetectedAt());
            fields.put("imageSha256", offer.getImageSha256());
        }
        fields.values().removeIf(Objects::isNull);

        apiClient.postImage("/plugin/bingo/" + offer.getBoard().getId() + "/submissions", fields, "image",
                offer.getPng(),
                response -> {
                    SubmissionResultData result = apiClient.deserialize(response.getBody(), SubmissionResultData.class);
                    remove(offer);
                    onDone.accept(result == null ? "Sent" : result.getMessage());
                },
                response -> onDone.accept(failureMessage(response)));
    }

    private String failureMessage(ApiResponse response) {
        switch (response.getStatusCode()) {
            case 403:
                return "You need to be on a team on this board";
            case 400:
            case 409:
                return apiClient.failureMessage(response, "Proof not accepted");
            default:
                return apiClient.failureMessage(response, "Couldn't send the proof");
        }
    }

    private void offer(PluginBoardData board, PluginTileData tile, String reason, Detection detection) {
        if (!config.offerProof() || !isOpenForMyTeam(board, tile) || tile.isProofPending()
                || !offeredTiles.add(key(board.getId(), tile.getId()))) {
            return;
        }

        popup.show("Proof ready to send", Text.escapeJagex(tile.getTitle()) + "<br>"
                + Text.escapeJagex(board.getTitle()), () -> capture(board, tile, reason, detection));
    }

    private void capture(PluginBoardData board, PluginTileData tile, String reason, Detection detection) {
        if (board.getVerificationCode() == null || client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        XpReading xp = tile.getType() == TileType.SKILL_XP ? progressTracker.xpReading(board, tile).orElse(null) : null;
        long accountHash = client.getAccountHash();
        BufferedImage icon = xp == null ? null : skillIconManager.getSkillImage(xp.getSkill());
        drawManager.requestNextFrameListener(frame -> {
            BufferedImage screenshot = ProofImage.stamp(frame, board.getVerificationCode());
            if (xp != null) {
                ProofImage.drawXpCard(screenshot, xp, tile.getXpTarget(), icon);
            }
            executor.execute(() -> save(board, tile, reason, detection, xp, accountHash, screenshot));
        });
    }

    private void save(PluginBoardData board, PluginTileData tile, String reason, Detection detection, XpReading xp,
                      long accountHash, BufferedImage screenshot) {
        byte[] png = png(screenshot);
        ProofOffer offer = new ProofOffer(UUID.randomUUID().toString(), board, tile, reason, screenshot, png,
                sha256(png), xp, accountHash, detection);
        enqueue(offer);
        if (detection != null) {
            chatMessageManager.queue(QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage("Lumbridge Guide: proof for " + Text.escapeJagex(tile.getTitle())
                            + " is ready to send from the panel")
                    .build());
        }
        store.save(new SavedProof(
                offer.getId(),
                board.getId(),
                tile.getId(),
                reason,
                xp == null ? null : xp.getSkill().name(),
                xp == null ? null : xp.getGained(),
                xp == null ? null : xp.getTotal(),
                accountHash,
                System.currentTimeMillis(),
                offer.getImageSha256(),
                detection), png);
    }

    private static byte[] png(BufferedImage image) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            return buffer.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return String.format("%064x", new BigInteger(1, MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Integer matchedItem(PluginTileData tile, Set<Integer> dropped) {
        if (tile.getItems() == null) {
            return null;
        }

        for (TileItemEntry item : tile.getItems()) {
            if (dropped.contains(ItemVariationMapping.map(item.getId()))) {
                return item.getId();
            }
        }
        return null;
    }

    private static boolean isOpenForMyTeam(PluginBoardData board, PluginTileData tile) {
        PluginTeamData team = board.getMyTeam();
        return team != null && !tile.isClaimedBy(team);
    }

    private static String key(String boardId, String tileId) {
        return boardId + ":" + tileId;
    }
}
