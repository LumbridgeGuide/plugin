package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginTileData;

/**
 * Wording shared by the tile list and the expanded tile view.
 */
final class TileText {

    static String badgeLabel(PluginTileData tile) {
        if (tile.getType() == null) {
            return "GOAL";
        }
        switch (tile.getType()) {
            case "skill_xp":
                return "XP";
            case "item_drop":
                return "DROP";
            case "kill_count":
                return "KC";
            default:
                return "GOAL";
        }
    }

    static String typeName(PluginTileData tile) {
        if (tile.getType() == null) {
            return "Custom goal";
        }
        switch (tile.getType()) {
            case "skill_xp":
                return "Skill XP";
            case "item_drop":
                return "Item drop";
            case "kill_count":
                return "Kill count";
            default:
                return "Custom goal";
        }
    }

    static String capitalise(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
    }

    private TileText() {
    }
}
