package com.lumbridgeguide;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(LumbridgeGuideConfig.CONFIG_GROUP)
public interface LumbridgeGuideConfig extends Config {

    String CONFIG_GROUP = "lumbridge_guide";

    @ConfigSection(
            name = "API Settings",
            description = "Configuration for the API connection",
            position = 0
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
