package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the player's progress on kill count and XP tiles and reports it so teammates can see it. Progress counts from
 * a baseline taken the first time the plugin sees the tile while its board is running: the XP at that moment, or the
 * kill count before the first kill it sees. Baselines are kept per RuneScape account, so switching accounts is safe.
 */
@Slf4j
@Singleton
public class TileProgressTracker {

    private static final Pattern KILL_COUNT_MESSAGE =
            Pattern.compile("Your (.+?) (?:kill|harvest|lap|completion) count is: ?([\\d,]+)");
    /** Baselines are kept in the RuneScape profile config under this prefix, one entry per board. */
    public static final String BASELINE_KEY_PREFIX = "tileProgressBaseline.";
    private static final Duration REPORT_INTERVAL = Duration.ofMinutes(1);
    private static final Type BASELINE_TYPE = new TypeToken<Map<String, Long>>() { }.getType();

    @Value
    public static class KillCount {
        String monster;
        long count;
    }

    /** A skill's XP for XP proof: what tracking counted since its baseline, and the skill's total XP now. */
    @Value
    public static class XpReading {
        Skill skill;
        Long gained;
        long total;
    }

    private final Client client;
    private final ConfigManager configManager;
    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;
    private final LumbridgeGuideClient apiClient;
    private final Gson gson;

    private final Map<String, Long> progress = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> dirtyTilesByBoard = new ConcurrentHashMap<>();
    private Instant lastReport = Instant.EPOCH;
    private volatile boolean seedingAfterLogin = true;
    private BiConsumer<PluginBoardData, PluginTileData> onTargetReached = (board, tile) -> { };

    @Inject
    public TileProgressTracker(Client client, ConfigManager configManager, LumbridgeGuideConfig config,
                               BoardDataService boardDataService, LumbridgeGuideClient apiClient, Gson gson) {
        this.client = client;
        this.configManager = configManager;
        this.config = config;
        this.boardDataService = boardDataService;
        this.apiClient = apiClient;
        this.gson = gson;
    }

    /** Called on the client thread when tracked progress first reaches a tile's target. */
    public void setOnTargetReached(BiConsumer<PluginBoardData, PluginTileData> listener) {
        onTargetReached = listener;
    }

    /** The player's tracked progress on a tile this session, fresher than what the last sync carried. */
    public OptionalLong progressFor(String tileId) {
        Long value = progress.get(tileId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    /** The player's XP on an XP tile right now. Gained is missing when tracking never took a baseline for the tile. */
    public Optional<XpReading> xpReading(PluginBoardData board, PluginTileData tile) {
        return skillOf(tile).map(skill -> {
            long total = client.getSkillExperience(skill);
            Long baseline = baselines(board.getId()).get(tile.getId());
            return new XpReading(skill, baseline == null ? null : Math.max(0, total - baseline), total);
        });
    }

    public boolean isTracking() {
        return config.trackTileProgress();
    }

    /**
     * Every skill reports a change as the player logs in, so this also takes the first reading after login. Readings
     * from before the first game tick only seed progress: a target already reached in an earlier session is not a new
     * completion, and offering it again would screenshot the welcome screen.
     */
    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (!config.trackTileProgress()) {
            return;
        }
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : tiles(board)) {
                if (!"skill_xp".equals(tile.getType())) {
                    continue;
                }
                skillOf(tile).ifPresent(skill ->
                        record(board, tile, client.getSkillExperience(skill), 0, !seedingAfterLogin));
            }
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if (event.getType() != ChatMessageType.GAMEMESSAGE || !config.trackTileProgress()) {
            return;
        }
        Optional<KillCount> killCount = parseKillCount(event.getMessage());
        if (killCount.isEmpty()) {
            return;
        }
        for (PluginBoardData board : boardDataService.getRunningBoards()) {
            for (PluginTileData tile : tiles(board)) {
                if ("kill_count".equals(tile.getType()) && tile.getMonsterName() != null
                        && tile.getMonsterName().equalsIgnoreCase(killCount.get().getMonster())) {
                    record(board, tile, killCount.get().getCount(), 1, true);
                }
            }
        }
    }

    /** Sends changed progress at most once a minute. */
    @Subscribe
    public void onGameTick(GameTick tick) {
        seedingAfterLogin = false;
        if (dirtyTilesByBoard.isEmpty() || !apiClient.hasApiKey()
                || Duration.between(lastReport, Instant.now()).compareTo(REPORT_INTERVAL) < 0) {
            return;
        }
        lastReport = Instant.now();
        for (String boardId : new ArrayList<>(dirtyTilesByBoard.keySet())) {
            Set<String> tileIds = dirtyTilesByBoard.remove(boardId);
            List<Map<String, Object>> entries = new ArrayList<>();
            for (String tileId : tileIds) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("tileId", tileId);
                entry.put("progress", progress.getOrDefault(tileId, 0L));
                entries.add(entry);
            }
            apiClient.post("/plugin/bingo/" + boardId + "/progress", Map.of("entries", entries),
                    response -> { },
                    response -> {
                        log.debug("Progress report failed with status {}", response.getStatusCode());
                        if (response.getStatusCode() == -1 || response.getStatusCode() >= 500) {
                            dirtyTilesByBoard.computeIfAbsent(boardId, id -> ConcurrentHashMap.newKeySet())
                                    .addAll(tileIds);
                        }
                    });
        }
    }

    /**
     * Parses messages such as "Your Vorkath kill count is: 32." into the monster and its count. Returns empty for any
     * other message.
     */
    public static Optional<KillCount> parseKillCount(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = KILL_COUNT_MESSAGE.matcher(message.replaceAll("<[^>]*>", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new KillCount(matcher.group(1).trim(), Long.parseLong(matcher.group(2).replace(",", ""))));
    }

    /**
     * Records a reading for a tile. The first reading on a board sets the baseline, less {@code alreadyCounted}: a
     * kill count message arrives after the kill, so that kill already counts towards the tile.
     */
    private void record(
            PluginBoardData board, PluginTileData tile, long reading, long alreadyCounted, boolean mayComplete) {
        Map<String, Long> baselines = baselines(board.getId());
        Long baseline = baselines.get(tile.getId());
        if (baseline == null) {
            baseline = reading - alreadyCounted;
            baselines.put(tile.getId(), baseline);
            saveBaselines(board.getId(), baselines);
        }
        long value = Math.max(0, reading - baseline);
        Long previous = progress.put(tile.getId(), value);
        if (previous == null || previous != value) {
            dirtyTilesByBoard.computeIfAbsent(board.getId(), id -> ConcurrentHashMap.newKeySet()).add(tile.getId());
        }
        long target = "kill_count".equals(tile.getType())
                ? (tile.getKillCount() == null ? 0 : tile.getKillCount())
                : tile.getXpTarget();
        if (mayComplete && target > 0 && value >= target && (previous == null || previous < target)) {
            onTargetReached.accept(board, tile);
        }
    }

    private static List<PluginTileData> tiles(PluginBoardData board) {
        return board.getTiles() == null ? List.of() : board.getTiles();
    }

    private static Optional<Skill> skillOf(PluginTileData tile) {
        if (tile.getSkill() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Skill.valueOf(tile.getSkill().toUpperCase(Locale.ROOT).replace(' ', '_')));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private Map<String, Long> baselines(String boardId) {
        String json = configManager.getRSProfileConfiguration(LumbridgeGuideConfig.CONFIG_GROUP,
                BASELINE_KEY_PREFIX + boardId);
        Map<String, Long> stored = json == null ? null : gson.fromJson(json, BASELINE_TYPE);
        return stored == null ? new HashMap<>() : new HashMap<>(stored);
    }

    private void saveBaselines(String boardId, Map<String, Long> baselines) {
        configManager.setRSProfileConfiguration(LumbridgeGuideConfig.CONFIG_GROUP, BASELINE_KEY_PREFIX + boardId,
                gson.toJson(baselines));
    }

    /** Forgets session progress on logout, so switching accounts starts clean. */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            progress.clear();
            dirtyTilesByBoard.clear();
            seedingAfterLogin = true;
        }
    }
}
