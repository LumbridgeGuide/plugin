package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@Slf4j
@Singleton
public class TileProgressTracker {

    public static final String BASELINE_KEY_PREFIX = "tileProgressBaseline.";
    private static final Duration REPORT_INTERVAL = Duration.ofMinutes(1);
    private static final Type BASELINE_TYPE = new TypeToken<Map<String, Long>>() { }.getType();

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
        this.counter = new TileProgressCounter(new BaselineStore() {
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

    public void setOnTargetReached(BiConsumer<Map.Entry<PluginBoardData, PluginTileData>, Detection> listener) {
        counter.setOnTargetReached(listener);
    }

    public Long progressFor(String tileId) {
        return counter.progressFor(tileId);
    }

    public Optional<XpReading> xpReading(PluginBoardData board, PluginTileData tile) {
        return tile.skill().map(skill -> {
            long total = client.getSkillExperience(skill);
            return new XpReading(skill, counter.xpGained(board.getId(), tile.getId(), total), total);
        });
    }

    public void rearm(String tileId) {
        counter.rearm(tileId);
    }

    public boolean isTracking() {
        return config.trackTileProgress();
    }

    @Subscribe
    public void onStatChanged(StatChanged event) {
        if (config.trackTileProgress()) {
            counter.xpSeen(boardDataService.getRunningBoards(), event.getSkill(), event.getXp(), !seedingAfterLogin);
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        if ((event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
                || !config.trackTileProgress()) {
            return;
        }
        KillCount.parse(event.getMessage())
                .ifPresent(killCount -> counter.killCountSeen(boardDataService.getRunningBoards(), killCount));
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event) {
        NPC npc = event.getNpc();
        if (npc != null && config.trackTileProgress()) {
            counter.lootReceived(boardDataService.getRunningBoards(), npc.getName(), npc.getId());
        }
    }

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
                entry.put("progress", Objects.requireNonNullElse(counter.progressFor(tileId), 0L));
                entries.add(entry);
            }
            apiClient.post("/plugin/bingo/" + boardId + "/progress", Map.of("entries", entries),
                    response -> { },
                    response -> {
                        log.debug("Progress report failed with status {}", response.getStatusCode());
                        if (response.getStatusCode() == ApiResponse.NETWORK_ERROR || response.getStatusCode() >= 500) {
                            counter.markChanged(boardId, tileIds);
                        }
                    });
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOGIN_SCREEN) {
            counter.clearSession();
            seedingAfterLogin = true;
        }
    }
}
