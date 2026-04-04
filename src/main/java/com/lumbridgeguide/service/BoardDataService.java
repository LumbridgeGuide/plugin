package com.lumbridgeguide.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginSyncResponse;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Manages bingo board data fetched from the backend, keeping the latest
 * response in memory.
 */
@Slf4j
@Singleton
public class BoardDataService {

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;

    private volatile PluginSyncResponse cachedData;

    @Inject
    public BoardDataService(LumbridgeGuideClient apiClient) {
        this.apiClient = apiClient;
        this.gson = new GsonBuilder().create();
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

    /**
     * Fetches board data from {@code GET /api/plugin/sync} asynchronously,
     * updating the in-memory data on success.
     * The optional callback is invoked after the request completes regardless of outcome.
     */
    public void refresh(Runnable onComplete) {
        if (!apiClient.isAuthenticated()) {
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

    /**
     * Blocking variant of {@link #refresh()} for use during plugin startup
     * or on background threads where the caller needs the result immediately.
     */
    public void refreshSync() {
        if (!apiClient.isAuthenticated()) {
            log.debug("Skipping board sync, no API key configured");
            return;
        }

        ApiResponse response = apiClient.getSync("/plugin/sync");
        if (response.isSuccess()) {
            handleSyncSuccess(response);
        } else {
            handleSyncFailure(response);
        }
    }

    private void handleSyncSuccess(ApiResponse response) {
        try {
            PluginSyncResponse syncResponse = gson.fromJson(response.getBody(), PluginSyncResponse.class);
            if (syncResponse != null) {
                cachedData = syncResponse;
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
}

