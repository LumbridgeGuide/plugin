package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginSyncResponse;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Board data is cached on disk so the panel has boards to show before the first refresh finishes. */
@Slf4j
@Singleton
public class BoardDataService {

    private static final File CACHE_DIR = new File(RuneLite.RUNELITE_DIR, "lumbridge-guide");
    private static final File CACHE_FILE = new File(CACHE_DIR, "board-cache.json");

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;

    private volatile PluginSyncResponse cachedData;

    @Inject
    public BoardDataService(LumbridgeGuideClient apiClient) {
        this.apiClient = apiClient;
        this.gson = new GsonBuilder().create();
        this.cachedData = loadFromDisk();
    }

    public List<PluginBoardData> getBoards() {
        if (cachedData == null || cachedData.getBoards() == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(cachedData.getBoards());
    }

    public Optional<PluginBoardData> getBoardById(String boardId) {
        return getBoards().stream()
                .filter(board -> board.getId().equals(boardId))
                .findFirst();
    }

    public boolean hasCachedData() {
        return cachedData != null
                && cachedData.getBoards() != null
                && !cachedData.getBoards().isEmpty();
    }

    public void refresh() {
        refresh(null);
    }

    /** {@code onComplete} runs whether or not the request succeeds. */
    public void refresh(Runnable onComplete) {
        if (!apiClient.hasApiKey()) {
            log.debug("Skipping board sync, no API key configured");
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        apiClient.get("/plugin/sync",
                response ->
                {
                    log.info("[LUMBY GUIDE] Sync successful, status code: {}", response.getStatusCode());
                    handleSyncSuccess(response);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                },
                response ->
                {
                    log.warn("[LUMBY GUIDE] Sync failed, status code: {}", response.getStatusCode());
                    handleSyncFailure(response);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                });
    }

    private void handleSyncSuccess(ApiResponse response) {
        try {
            PluginSyncResponse syncResponse = gson.fromJson(response.getBody(), PluginSyncResponse.class);
            if (syncResponse != null) {
                cachedData = syncResponse;
                saveToDisk(syncResponse);
                int boardCount = syncResponse.getBoards() != null ? syncResponse.getBoards().size() : 0;
                log.info("Board data synced: {} active board(s)", boardCount);
            }
        } catch (Exception exception) {
            log.warn("Failed to parse board sync response");
        }
    }

    private void handleSyncFailure(ApiResponse response) {
        if (hasCachedData()) {
            log.warn("Board sync failed (status {}), using cached data", response.getStatusCode());
        } else {
            log.warn("Board sync failed (status {}), no cached data available", response.getStatusCode());
        }
    }

    private PluginSyncResponse loadFromDisk() {
        if (!CACHE_FILE.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(CACHE_FILE, StandardCharsets.UTF_8)) {
            PluginSyncResponse loaded = gson.fromJson(reader, PluginSyncResponse.class);
            if (loaded != null && loaded.getBoards() != null) {
                log.info("Loaded {} cached board(s) from disk", loaded.getBoards().size());
            }
            return loaded;
        } catch (Exception exception) {
            log.warn("Failed to load board cache from disk");
            return null;
        }
    }

    private void saveToDisk(PluginSyncResponse data) {
        try {
            if (!CACHE_DIR.exists() && !CACHE_DIR.mkdirs()) {
                log.warn("Failed to create cache directory");
                return;
            }

            try (FileWriter writer = new FileWriter(CACHE_FILE, StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
            }
        } catch (IOException exception) {
            log.warn("Failed to save board cache to disk");
        }
    }
}

