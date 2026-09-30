package com.lumbridgeguide.notifications;

import com.google.gson.Gson;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.notifications.data.NotificationData;
import com.lumbridgeguide.notifications.data.NotificationPageData;
import com.lumbridgeguide.notifications.data.UnreadCountData;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.GameTick;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The player's Lumbridge Guide notifications: bingo invites they can answer in game, proof results and board news.
 * The unread count is checked every minute; the list is only fetched when the inbox is opened.
 */
@Singleton
public class InboxService {

    private static final Duration POLL_INTERVAL = Duration.ofMinutes(1);

    private final LumbridgeGuideClient apiClient;
    private final LumbridgeGuideConfig config;
    private final ChatMessageManager chatMessageManager;
    private final Gson gson;

    private Instant lastPoll = Instant.EPOCH;
    private volatile int unreadCount = -1;
    private IntConsumer onUnreadCount = count -> { };

    @Inject
    public InboxService(LumbridgeGuideClient apiClient, LumbridgeGuideConfig config,
                        ChatMessageManager chatMessageManager, Gson gson) {
        this.apiClient = apiClient;
        this.config = config;
        this.chatMessageManager = chatMessageManager;
        this.gson = gson;
    }

    public void setOnUnreadCount(IntConsumer listener) {
        onUnreadCount = listener;
        if (unreadCount >= 0) {
            listener.accept(unreadCount);
        }
    }

    /** Checks the unread count once a minute. */
    @Subscribe
    public void onGameTick(GameTick tick) {
        if (!apiClient.hasApiKey() || Duration.between(lastPoll, Instant.now()).compareTo(POLL_INTERVAL) < 0) {
            return;
        }
        lastPoll = Instant.now();
        refreshCount();
    }

    public void refreshCount() {
        apiClient.get("/plugin/notifications/unread-count", response -> {
            UnreadCountData data = gson.fromJson(response.getBody(), UnreadCountData.class);
            int count = data == null ? 0 : data.getCount();
            if (unreadCount >= 0 && count > unreadCount && config.notificationChatMessages()) {
                int fresh = count - unreadCount;
                chatMessageManager.queue(QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage("Lumbridge Guide: " + fresh + " new notification"
                                + (fresh == 1 ? "" : "s") + ". Open the panel's Inbox to see them.")
                        .build());
            }
            unreadCount = count;
            onUnreadCount.accept(count);
        }, response -> { });
    }

    public void load(Consumer<List<NotificationData>> onSuccess, Consumer<String> onFailure) {
        apiClient.get("/plugin/notifications",
                response -> {
                    NotificationPageData page = gson.fromJson(response.getBody(), NotificationPageData.class);
                    onSuccess.accept(page == null || page.getNotifications() == null
                            ? List.of() : page.getNotifications());
                },
                response -> onFailure.accept(response.getStatusCode() == 401
                        ? "Check your API key in the plugin settings"
                        : "Could not load notifications"));
    }

    public void act(String notificationId, String action, Runnable onDone, Consumer<String> onFailure) {
        apiClient.post("/plugin/notifications/" + notificationId + "/action", Map.of("action", action),
                response -> {
                    refreshCount();
                    onDone.run();
                },
                response -> onFailure.accept("That didn't work. Try again on the website."));
    }

    public void markAllRead(Runnable onDone) {
        apiClient.post("/plugin/notifications/read-all", Map.of(),
                response -> {
                    unreadCount = 0;
                    onUnreadCount.accept(0);
                    onDone.run();
                },
                response -> { });
    }
}
