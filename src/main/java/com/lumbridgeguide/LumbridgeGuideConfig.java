package com.lumbridgeguide;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

import java.awt.Color;

@ConfigGroup(LumbridgeGuideConfig.CONFIG_GROUP)
public interface LumbridgeGuideConfig extends Config {

    String CONFIG_GROUP = "lumbridge_guide";
    String ACCENT_COLOUR_KEY = "accentColour";

    @ConfigSection(
            name = "Appearance",
            description = "How the Lumbridge Guide panel looks",
            position = 0
    )
    String appearanceSection = "appearanceSection";

    @ConfigItem(
            keyName = ACCENT_COLOUR_KEY,
            name = "Accent colour",
            description = "The highlight colour for selected tabs, buttons and verification codes",
            section = appearanceSection,
            position = 0
    )
    default Color accentColour() {
        return new Color(0xE07A3A);
    }

    @ConfigSection(
            name = "Bingo",
            description = "Bingo board display settings",
            position = 1,
            closedByDefault = true
    )
    String bingoSection = "bingoSection";

    @ConfigItem(
            keyName = "showTeamPrefix",
            name = "Show Team Prefix",
            description = "Prefix your name in chat with your bingo team name in the team colour",
            section = bingoSection,
            position = 0
    )
    default boolean showTeamPrefix() {
        return false;
    }

    @ConfigItem(
            keyName = "unclaimedFirst",
            name = "Unclaimed tiles first",
            description = "List tiles you still have to claim above the ones already claimed",
            section = bingoSection,
            position = 1
    )
    default boolean unclaimedFirst() {
        return false;
    }

    @ConfigItem(
            keyName = "trackTileProgress",
            name = "Track tile progress",
            description = "Count your kills and XP towards kill count and XP tiles, and share the count with your team",
            section = bingoSection,
            position = 3
    )
    default boolean trackTileProgress() {
        return true;
    }

    @Range(max = 60)
    @Units(Units.MINUTES)
    @ConfigItem(
            keyName = "boardRefreshMinutes",
            name = "Refresh boards every",
            description = "How often to fetch your boards while logged in, so claims made on the website show up. "
                    + "0 turns it off",
            section = bingoSection,
            position = 2
    )
    default int boardRefreshMinutes() {
        return 5;
    }

    @ConfigSection(
            name = "Gear",
            description = "Bank tag tabs made from gear configs",
            position = 2,
            closedByDefault = true
    )
    String gearSection = "gearSection";

    @ConfigItem(
            keyName = "includeMissingItems",
            name = "Include missing items",
            description = "Start with Include missing items ticked when making a tag tab",
            section = gearSection,
            position = 0
    )
    default boolean includeMissingItems() {
        return true;
    }

    @ConfigSection(
            name = "Account",
            description = "RuneScape account syncing",
            position = 3,
            closedByDefault = true
    )
    String accountSection = "accountSection";

    @ConfigItem(
            keyName = "syncOnLoginLogout",
            name = "Sync on login and logout",
            description = "Also sync your stats and quests when you log in and out. Only for accounts you have already linked with Sync now.",
            section = accountSection,
            position = 0
    )
    default boolean syncOnLoginLogout() {
        return false;
    }

    @ConfigSection(
            name = "API Settings",
            description = "Configuration for the API connection",
            position = 999,
            closedByDefault = true
    )
    String apiSection = "apiSection";

    @ConfigItem(
            keyName = "apiKey",
            name = "API Key",
            description = "Your API key from the Lumbridge Guide website. Keep this private!",
            secret = true,
            section = apiSection,
            position = 0
    )
    default String apiKey() {
        return "";
    }

}
