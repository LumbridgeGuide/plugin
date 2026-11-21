package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BoardDataService {

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;

    private volatile PluginSyncResponse cachedData;

    public List<PluginBoardData> getBoards() {
        if (cachedData == null || cachedData.getBoards() == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(cachedData.getBoards());
    }

    public List<PluginBoardData> getRunningBoards() {
        Instant now = Instant.now();
        return getBoards().stream()
                .filter(board -> reached(now, board.getStartsAt()) && !reached(now, board.getEndsAt()))
                .collect(Collectors.toList());
    }

    private static boolean reached(Instant now, String isoInstant) {
        if (isoInstant == null || isoInstant.isEmpty()) {
            return false;
        }
        try {
            return !now.isBefore(Instant.parse(isoInstant));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public boolean hasCachedData() {
        return cachedData != null
                && cachedData.getBoards() != null
                && !cachedData.getBoards().isEmpty();
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(Runnable onFinished) {
        Runnable done = onFinished == null ? () -> { } : onFinished;
        if (!apiClient.hasApiKey()) {
            log.debug("Skipping board sync, no API key configured");
            cachedData = null;
            done.run();
            return;
        }

        apiClient.get("/plugin/sync",
                response ->
                {
                    handleSyncSuccess(response);
                    done.run();
                },
                response ->
                {
                    handleSyncFailure(response);
                    done.run();
                });
    }

    private void handleSyncSuccess(ApiResponse response) {
        try {
            PluginSyncResponse syncResponse = gson.fromJson(response.getBody(), PluginSyncResponse.class);
            if (syncResponse != null) {
                cachedData = syncResponse;
                int boardCount = syncResponse.getBoards() != null ? syncResponse.getBoards().size() : 0;
                log.debug("Board data synced: {} active board(s)", boardCount);
            }
        } catch (Exception exception) {
            log.warn("Failed to parse board sync response", exception);
        }
    }

    private void handleSyncFailure(ApiResponse response) {
        if (response.getStatusCode() == 401) {
            log.warn("Board sync rejected the API key, clearing boards");
            cachedData = null;
            return;
        }
        log.warn("Board sync failed (status {})", response.getStatusCode());
    }
}

