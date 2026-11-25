package com.lumbridgeguide.bingo;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginPollRequest;
import com.lumbridgeguide.bingo.data.PluginPollResponse;
import com.lumbridgeguide.bingo.data.PluginProofResult;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class BoardDataService {

    private static final long DEFAULT_POLL_SECONDS = 150;

    private final LumbridgeGuideClient apiClient;
    private final Gson gson;
    private final LumbridgeGuideConfig config;
    private final BingoChatAnnouncer announcer;

    private final Map<String, PluginBoardData> boards = new LinkedHashMap<>();
    private final Map<String, Long> versions = new HashMap<>();
    private final List<Long> unacknowledged = new ArrayList<>();
    private final List<Runnable> waiting = new ArrayList<>();
    private Long cursor;
    private boolean loaded;
    private boolean polling;

    @Getter
    private volatile long nextPollSeconds = DEFAULT_POLL_SECONDS;

    public synchronized List<PluginBoardData> getBoards() {
        return Collections.unmodifiableList(new ArrayList<>(boards.values()));
    }

    public PluginBoardData getSelectedBoard() {
        List<PluginBoardData> current = getBoards();
        return current.stream().filter(board -> config.selectedBoardId().equals(board.getId())).findFirst()
                .orElse(current.isEmpty() ? null : current.get(0));
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

    public synchronized boolean hasCachedData() {
        return loaded;
    }

    public synchronized void clear() {
        boards.clear();
        versions.clear();
        unacknowledged.clear();
        cursor = null;
        loaded = false;
    }

    public void refresh() {
        refresh(null);
    }

    public void refresh(Runnable onFinished) {
        Runnable done = onFinished == null ? () -> { } : onFinished;
        if (!apiClient.hasApiKey()) {
            clear();
            done.run();
            return;
        }

        PluginPollRequest request;
        synchronized (this) {
            waiting.add(done);
            if (polling) {
                return;
            }
            polling = true;
            request = new PluginPollRequest(new HashMap<>(versions), cursor, List.copyOf(unacknowledged));
        }
        apiClient.post("/plugin/poll", request, this::handlePoll, this::handleFailure);
    }

    private void handlePoll(ApiResponse response) {
        PluginPollResponse poll = gson.fromJson(response.getBody(), PluginPollResponse.class);
        boolean firstPoll;
        List<PluginBoardData> current;
        synchronized (this) {
            firstPoll = cursor == null;
            merge(poll);
            current = List.copyOf(boards.values());
        }

        announcer.announce(poll, firstPoll, current);
        finish();
    }

    private void merge(PluginPollResponse poll) {
        loaded = true;
        cursor = poll.getCursor();
        nextPollSeconds = poll.getNextPollSeconds() > 0 ? poll.getNextPollSeconds() : DEFAULT_POLL_SECONDS;
        unacknowledged.clear();
        if (!poll.isChanged()) {
            return;
        }

        versions.clear();
        versions.putAll(poll.getVersions() == null ? Map.of() : poll.getVersions());
        if (poll.getBoards() != null) {
            poll.getBoards().forEach(board -> boards.put(board.getId(), board));
        }
        boards.keySet().retainAll(versions.keySet());
        if (poll.getResults() != null) {
            poll.getResults().stream().map(PluginProofResult::getId).forEach(unacknowledged::add);
        }
    }

    private void handleFailure(ApiResponse response) {
        if (response.getStatusCode() == 401) {
            log.warn("Board poll rejected the plugin key, clearing boards");
            clear();
        } else {
            log.warn("Board poll failed (status {})", response.getStatusCode());
        }
        finish();
    }

    private void finish() {
        List<Runnable> callbacks;
        synchronized (this) {
            polling = false;
            callbacks = List.copyOf(waiting);
            waiting.clear();
        }
        callbacks.forEach(Runnable::run);
    }
}
