package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.config.ConfigManager;

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
    private static final String BASELINE_KEY_PREFIX = "tileProgressBaseline.";
    private static final Duration REPORT_INTERVAL = Duration.ofMinutes(1);
    private static final Type BASELINE_TYPE = new TypeToken<Map<String, Long>>() { }.getType();

    @Value
    public static class KillCount {
        String monster;
        long count;
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

    /** The player's tracked progress on a tile this session, fresher than what the last sync carried. */
    public OptionalLong progressFor(String tileId) {
        Long value = progress.get(tileId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public boolean isTracking() {
        return config.trackTileProgress();
    }

    /** Reads XP tiles again. Called on the client thread when a stat changes and when the player logs in. */
    public void onStatsChanged() {
        if (!config.trackTileProgress()) {
            return;
        }
        for (PluginBoardData board : runningBoards()) {
            for (PluginTileData tile : tiles(board)) {
                if (!"skill_xp".equals(tile.getType())) {
                    continue;
                }
                skillOf(tile).ifPresent(skill -> record(board, tile, client.getSkillExperience(skill), 0));
            }
        }
    }

    /** Reads a kill count chat message. Called on the client thread for game messages. */
    public void onGameMessage(String message) {
        if (!config.trackTileProgress()) {
            return;
        }
        Optional<KillCount> killCount = parseKillCount(message);
        if (killCount.isEmpty()) {
            return;
        }
        for (PluginBoardData board : runningBoards()) {
            for (PluginTileData tile : tiles(board)) {
                if ("kill_count".equals(tile.getType()) && tile.getMonsterName() != null
                        && tile.getMonsterName().equalsIgnoreCase(killCount.get().getMonster())) {
                    record(board, tile, killCount.get().getCount(), 1);
                }
            }
        }
    }

    /** Sends changed progress at most once a minute. Called every game tick. */
    public void reportIfDue() {
        if (dirtyTilesByBoard.isEmpty() || Duration.between(lastReport, Instant.now()).compareTo(REPORT_INTERVAL) < 0) {
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
                    response -> log.debug("Progress report failed with status {}", response.getStatusCode()));
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
    private void record(PluginBoardData board, PluginTileData tile, long reading, long alreadyCounted) {
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
    }

    private List<PluginBoardData> runningBoards() {
        Instant now = Instant.now();
        List<PluginBoardData> running = new ArrayList<>();
        for (PluginBoardData board : boardDataService.getBoards()) {
            if (isAfter(now, board.getStartsAt()) && !isAfter(now, board.getEndsAt())) {
                running.add(board);
            }
        }
        return running;
    }

    private static boolean isAfter(Instant now, String isoInstant) {
        if (isoInstant == null || isoInstant.isEmpty()) {
            return false;
        }
        try {
            return !now.isBefore(Instant.parse(isoInstant));
        } catch (RuntimeException ignored) {
            return false;
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

    /** Forgets session progress, for example after switching accounts. */
    public void reset() {
        progress.clear();
        dirtyTilesByBoard.clear();
    }
}
