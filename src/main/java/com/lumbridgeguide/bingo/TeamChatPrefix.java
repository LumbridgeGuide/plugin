package com.lumbridgeguide.bingo;

import lombok.experimental.UtilityClass;
import net.runelite.client.util.Text;

import java.util.regex.Pattern;

@UtilityClass
public class TeamChatPrefix {

    private static final Pattern HEX_COLOUR = Pattern.compile("[0-9a-fA-F]{6}");

    public static String of(String teamName, String colour) {
        String tag = "[" + Text.escapeJagex(teamName) + "]";
        String hex = colour == null ? "" : colour.replace("#", "");
        if (!HEX_COLOUR.matcher(hex).matches()) {
            return tag + " ";
        }
        return "<col=" + hex + ">" + tag + "</col> ";
    }
}
