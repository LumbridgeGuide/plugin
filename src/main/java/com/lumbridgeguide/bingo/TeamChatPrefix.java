package com.lumbridgeguide.bingo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.client.util.Text;

import java.util.regex.Pattern;

/**
 * Builds the "[Team] " chat prefix. Team names are chosen by board hosts, so they are escaped before going into chat
 * markup, and a colour that is not plain hex is dropped rather than written into a {@code <col>} tag.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TeamChatPrefix {

    private static final Pattern HEX_COLOR = Pattern.compile("[0-9a-fA-F]{6}");

    public static String of(String teamName, String color) {
        String tag = "[" + Text.escapeJagex(teamName) + "]";
        String hex = color == null ? "" : color.replace("#", "");
        if (!HEX_COLOR.matcher(hex).matches()) {
            return tag + " ";
        }
        return "<col=" + hex + ">" + tag + "</col> ";
    }
}
