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
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Feeds game events to a {@link TileProgressCounter} and reports the progress so teammates can see it. Baselines are
 * kept in the RuneScape profile config, so each account has its own and switching accounts is safe.
 */
@Slf4j
@Singleton
public class TileProgressTracker {

    /** Baselines are kept in the RuneScape profile config under this prefix, one entry per board. */
    public static final String BASELINE_KEY_PREFIX = "tileProgressBaseline.";
    private static final Duration REPORT_INTERVAL = Duration.ofMinutes(1);
    private static final Type BASELINE_TYPE = new TypeToken<Map<String, Long>>() { }.getType();

    /** A skill's XP for XP proof: what tracking counted since its baseline, and the skill's total XP now. */
    @Value
    public static class XpReading {
        Skill skill;
        Long gained;
        long total;
    }

    private final Client client;
    private final LumbridgeGuideConfig config;
    private final BoardDataService boardDataService;
    private final LumbridgeGuideClient apiClient;
    private final TileProgressCounter counter;

    private Instant lastReport = Instant.EPOCH;
    private volatile boolean seedingAfterLogin = true;

    @Inject
    public TileProgressTracker(Client client, ConfigManager configManager, LumbridgeGuideConfig config,
                               BoardDataService boardDataService, LumbridgeGuideClient apiClient, Gson gson) {
        this.client = client;
        this.config = config;
        this.boardDataService = boardDataService;
        this.apiClient = apiClient;
        this.counter = new TileProgressCounter(new TileProgressCounter.BaselineStore() {
            @Override
            public Map<String, Long> load(String boardId) {
                String json = configManager.getRSProfileConfiguration(LumbridgeGuideConfig.CONFIG_GROUP,
                        BASELINE_KEY_PREFIX + boardId);
                Map<String, Long> stored = json == null ? null : gson.fromJson(json, BASELINE_TYPE);
                return stored == null ? new HashMap<>() : new HashMap<>(stored);
            }

            @Override
            public void save(String boardId, Map<String, Long> baselines) {
                configManager.setRSProfileConfiguration(LumbridgeGuideConfig.CONFIG_GROUP,
                        BASELINE_KEY_PREFIX + boardId, gson.toJson(baselines));
            }
        });
    }

    /** Called on the client thread when tracked progress first reaches a tile's target. */
    public void setOnTargetReached(BiConsumer<PluginBoardData, PluginTileData> listener) {
        counter.setOnTargetReached(listener);
    }

    /** The player's tracked progress on a tile this session, fresher than what the last sync carried. */
    public OptionalLong progressFor(String tileId) {
        return counter.progressFor(tileId);
    }

    /** The player's XP on an XP tile right now. Gained is missing when tracking never took a baseline for the tile. */
    public Optional<XpReading> xpReading(PluginBoardData board, PluginTileData tile) {
        return TileProgressCounter.skillOf(tile).map(skill -> {
            long total = client.getSkillExperience(skill);
            return new XpReading(skill, counter.xpGained(board.getId(), tile.getId(), total), total);
        });
    }

    /** Forgets that a tile's target was reached, so the next reading at or past it counts as a completion again. */
    public void rearm(String tileId) {
        counter.rearm(tileId);
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
        if (config.trackTileProgress()) {
            counter.xpSeen(boardDataService.getRunningBoards(), event.getSkill(), event.getXp(), !seedingAfterLogin);
        }
    }

    /** Kill count messages arrive as game messages, or as spam when the player filters those. */
    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if ((event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
                || !config.trackTileProgress()) {
            return;
        }
        TileProgressCounter.parseKillCount(event.getMessage())
                .ifPresent(killCount -> counter.killCountSeen(boardDataService.getRunningBoards(), killCount));
    }

    /** Monsters without a kill count message are counted from their loot. */
    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        NPC npc = event.getNpc();
        if (npc != null && config.trackTileProgress()) {
            counter.lootReceived(boardDataService.getRunningBoards(), npc.getName(), npc.getId());
        }
    }

    /** Sends changed progress at most once a minute. */
    @Subscribe
    public void onGameTick(GameTick tick) {
        seedingAfterLogin = false;
        if (!counter.hasChanges() || !apiClient.hasApiKey()
                || Duration.between(lastReport, Instant.now()).compareTo(REPORT_INTERVAL) < 0) {
            return;
        }
        lastReport = Instant.now();
        for (Map.Entry<String, Set<String>> changed : counter.takeChanged().entrySet()) {
            String boardId = changed.getKey();
            Set<String> tileIds = changed.getValue();
            List<Map<String, Object>> entries = new ArrayList<>();
            for (String tileId : tileIds) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("tileId", tileId);
                entry.put("progress", counter.progressFor(tileId).orElse(0L));
                entries.add(entry);
            }
            apiClient.post("/plugin/bingo/" + boardId + "/progress", Map.of("entries", entries),
                    response -> { },
                    response -> {
                        log.debug("Progress report failed with status {}", response.getStatusCode());
                        if (response.getStatusCode() == -1 || response.getStatusCode() >= 500) {
                            counter.markChanged(boardId, tileIds);
                        }
                    });
        }
    }

    /** Forgets session progress on logout, so switching accounts starts clean. */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            counter.clearSession();
            seedingAfterLogin = true;
        }
    }
}
