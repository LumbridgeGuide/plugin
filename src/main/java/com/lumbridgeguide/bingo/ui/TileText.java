package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.experimental.UtilityClass;

@UtilityClass
class TileText {

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
}
