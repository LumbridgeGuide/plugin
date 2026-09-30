package com.lumbridgeguide.stars;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Spots a crashed shooting star next to the player, reports it to Lumbridge Guide on request, and lists the stars
 * other players have reported.
 */
@Singleton
public class ShootingStarService {

    private static final Map<Integer, Integer> STAR_SIZES = Map.of(
            ObjectID.STAR_SIZE_ONE_STAR, 1,
            ObjectID.STAR_SIZE_TWO_STAR, 2,
            ObjectID.STAR_SIZE_THREE_STAR, 3,
            ObjectID.STAR_SIZE_FOUR_STAR, 4,
            ObjectID.STAR_SIZE_FIVE_STAR, 5,
            ObjectID.STAR_SIZE_SIX_STAR, 6,
            ObjectID.STAR_SIZE_SEVEN_STAR, 7,
            ObjectID.STAR_SIZE_EIGHT_STAR, 8,
            ObjectID.STAR_SIZE_NINE_STAR, 9);
    private static final Type STAR_LIST = new TypeToken<List<ShootingStarData>>() { }.getType();

    private final Client client;
    private final LumbridgeGuideClient apiClient;
    private final LumbridgeGuideConfig config;
    private final ChatMessageManager chatMessageManager;
    private final Gson gson;

    private volatile ShootingStarData nearbyStar;
    private volatile WorldPoint playerLocation;
    private Consumer<ShootingStarData> onNearbyStar = star -> { };

    @Inject
    public ShootingStarService(Client client, LumbridgeGuideClient apiClient, LumbridgeGuideConfig config,
                               ChatMessageManager chatMessageManager, Gson gson) {
        this.client = client;
        this.apiClient = apiClient;
        this.config = config;
        this.chatMessageManager = chatMessageManager;
        this.gson = gson;
    }

    /** Called with the star next to the player, or null once it is gone. */
    public void setOnNearbyStar(Consumer<ShootingStarData> listener) {
        onNearbyStar = listener;
        listener.accept(nearbyStar);
    }

    public ShootingStarData getNearbyStar() {
        return nearbyStar;
    }

    public WorldPoint getPlayerLocation() {
        return playerLocation;
    }

    /** Remembers where the player is, for distances in the list. */
    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client.getLocalPlayer() != null) {
            playerLocation = client.getLocalPlayer().getWorldLocation();
        }
    }

    /** A star spawns again each time it shrinks a size. */
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject object = event.getGameObject();
        Integer size = STAR_SIZES.get(object.getId());
        if (size == null) {
            return;
        }
        boolean fresh = nearbyStar == null;
        ShootingStarData star = new ShootingStarData();
        star.setWorld(client.getWorld());
        star.setX(object.getWorldLocation().getX());
        star.setY(object.getWorldLocation().getY());
        star.setPlane(object.getWorldLocation().getPlane());
        star.setLevel(size);
        nearbyStar = star;
        onNearbyStar.accept(star);
        if (fresh && config.promptNearbyStars()) {
            chatMessageManager.queue(QueuedMessage.builder()
                    .type(ChatMessageType.CONSOLE)
                    .runeLiteFormattedMessage("Lumbridge Guide: a size " + size
                            + " shooting star is next to you. Report it from the panel's Stars tab.")
                    .build());
        }
    }

    /** A shrinking star can spawn its next size before the old one despawns, so only the size showing clears it. */
    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        GameObject object = event.getGameObject();
        ShootingStarData star = nearbyStar;
        Integer size = STAR_SIZES.get(object.getId());
        if (star != null && size != null && size == star.getLevel()
                && object.getWorldLocation().getX() == star.getX() && object.getWorldLocation().getY() == star.getY()) {
            nearbyStar = null;
            onNearbyStar.accept(null);
        }
    }

    public void report(ShootingStarData star, Consumer<String> onDone) {
        Map<String, Integer> body = Map.of("world", star.getWorld(), "x", star.getX(), "y", star.getY(),
                "plane", star.getPlane(), "level", star.getLevel());
        apiClient.post("/plugin/shooting-stars", body,
                response -> onDone.accept("Reported. Thanks!"),
                response -> onDone.accept(response.getStatusCode() == 401
                        ? "Check your API key in the plugin settings" : "Couldn't report the star"));
    }

    public void list(Consumer<List<ShootingStarData>> onSuccess, Consumer<String> onFailure) {
        apiClient.get("/map/shooting-stars",
                response -> {
                    List<ShootingStarData> stars = gson.fromJson(response.getBody(), STAR_LIST);
                    onSuccess.accept(stars == null ? List.of() : stars);
                },
                response -> onFailure.accept("Couldn't load the stars"));
    }
}
