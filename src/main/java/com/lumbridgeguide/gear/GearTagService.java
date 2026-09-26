package com.lumbridgeguide.gear;

import com.google.gson.Gson;
import com.lumbridgeguide.api.ApiResponse;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.gear.data.PluginGearData;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.plugins.banktags.TagManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a bank tag from a Lumbridge Guide gear config. The tag is named after
 * the config and holds every item in its equipment, inventory and rune pouch.
 * Syncing the same config again replaces the tag so it mirrors the current gear.
 */
@Slf4j
@Singleton
public class GearTagService {

    private static final Pattern GEAR_CODE = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String FALLBACK_TAG = "gear";

    private final LumbridgeGuideClient apiClient;
    private final TagManager tagManager;
    private final ClientThread clientThread;
    private final Gson gson = new Gson();

    @Inject
    public GearTagService(LumbridgeGuideClient apiClient, TagManager tagManager, ClientThread clientThread) {
        this.apiClient = apiClient;
        this.tagManager = tagManager;
        this.clientThread = clientThread;
    }

    @Value
    public static class Result {
        boolean success;
        String message;
    }

    /**
     * Accepts either a bare gear code or a full gear page link.
     */
    public static Optional<String> extractCode(String input) {
        if (input == null) {
            return Optional.empty();
        }
        Matcher matcher = GEAR_CODE.matcher(input.trim());
        return matcher.find() ? Optional.of(matcher.group().toLowerCase(Locale.ROOT)) : Optional.empty();
    }

    public static String tagName(String gearName) {
        String slug = gearName == null ? "" : gearName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isEmpty() ? FALLBACK_TAG : slug;
    }

    public void sync(String code, Consumer<Result> onComplete) {
        apiClient.get("/plugin/gear/" + code,
                response -> applyTag(response, onComplete),
                response -> onComplete.accept(new Result(false, failureMessage(response))));
    }

    private void applyTag(ApiResponse response, Consumer<Result> onComplete) {
        PluginGearData gear;
        try {
            gear = gson.fromJson(response.getBody(), PluginGearData.class);
        } catch (Exception exception) {
            gear = null;
        }
        if (gear == null || gear.getItemIds() == null || gear.getItemIds().isEmpty()) {
            onComplete.accept(new Result(false, "That gear set has no items"));
            return;
        }

        PluginGearData gearData = gear;
        String tag = tagName(gearData.getName());
        clientThread.invoke(() ->
        {
            tagManager.removeTag(tag);
            for (int itemId : gearData.getItemIds()) {
                tagManager.addTag(itemId, tag, false);
            }
            log.info("Bank tag '{}' synced with {} item(s)", tag, gearData.getItemIds().size());
            onComplete.accept(new Result(true,
                    "Tagged " + gearData.getItemIds().size() + " items as \"" + tag + "\""));
        });
    }

    private static String failureMessage(ApiResponse response) {
        switch (response.getStatusCode()) {
            case 404:
                return "No gear set found for that code";
            case 401:
                return "Check your API key in the plugin settings";
            case -1:
                return "Could not reach Lumbridge Guide";
            default:
                return "Sync failed (" + response.getStatusCode() + ")";
        }
    }
}
