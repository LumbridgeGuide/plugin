package com.lumbridgeguide;

import com.lumbridgeguide.bingo.ClaimMessages;
import com.lumbridgeguide.ui.Theme;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

import java.awt.Color;

@ConfigGroup(LumbridgeGuideConfig.CONFIG_GROUP)
public interface LumbridgeGuideConfig extends Config {

    String CONFIG_GROUP = "lumbridge_guide";
    String ACCENT_COLOUR_KEY = "accentColour";
    String API_KEY_KEY = "apiKey";

    @ConfigSection(
            name = "Appearance",
            description = "How the Lumbridge Guide panel looks",
            position = 0
    )
    String appearanceSection = "appearanceSection";

    @ConfigItem(
            keyName = ACCENT_COLOUR_KEY,
            name = "Accent colour",
            description = "The highlight colour for selected tabs, buttons and board codes",
            section = appearanceSection,
            position = 0
    )
    default Color accentColour() {
        return Theme.DEFAULT_ACCENT;
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
            name = "Show team prefix",
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
            description = "Count your kills and XP towards kill count and XP tiles on your running boards. Sends "
                    + "Lumbridge Guide your progress on those tiles once a minute so your team can see it",
            section = bingoSection,
            position = 3
    )
    default boolean trackTileProgress() {
        return false;
    }

    @ConfigItem(
            keyName = "offerProof",
            name = "Offer to send proof",
            description = "When a drop, kill count or XP target matches an open tile, take a screenshot with the board "
                    + "code and offer to send it as proof",
            section = bingoSection,
            position = 4
    )
    default boolean offerProof() {
        return true;
    }

    @ConfigItem(
            keyName = "claimMessages",
            name = "Claim messages",
            description = "Which teams' tile claims to announce in chat, with the board name in front",
            section = bingoSection,
            position = 2
    )
    default ClaimMessages claimMessages() {
        return ClaimMessages.ALL_TEAMS;
    }

    @ConfigItem(keyName = "selectedBoardId", name = "", description = "", hidden = true)
    default String selectedBoardId() {
        return "";
    }

    @ConfigItem(keyName = "selectedBoardId", name = "", description = "")
    void setSelectedBoardId(String boardId);

    @ConfigSection(
            name = "Account",
            description = "RuneScape account syncing",
            position = 5,
            closedByDefault = true
    )
    String accountSection = "accountSection";

    @ConfigItem(
            keyName = "syncOnLoginLogout",
            name = "Sync on login and logout",
            description = "Also sync your stats and quests when you log in and out."
                    + " Only for accounts you have already linked with Sync now",
            section = accountSection,
            position = 1
    )
    default boolean syncOnLoginLogout() {
        return false;
    }

    @ConfigItem(
            keyName = API_KEY_KEY,
            name = "Plugin key",
            description = "Your plugin key from the Lumbridge Guide website. Keep it private",
            secret = true,
            section = accountSection,
            position = 0
    )
    default String apiKey() {
        return "";
    }

}
