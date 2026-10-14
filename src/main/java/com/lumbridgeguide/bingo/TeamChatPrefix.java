package com.lumbridgeguide.bingo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.runelite.client.util.Text;

import java.util.regex.Pattern;

/** Escaped, colour-safe bingo team tag placed before the player's name in chat */
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
