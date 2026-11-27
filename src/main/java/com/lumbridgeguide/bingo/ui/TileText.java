package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TileText {

    public static String typeName(PluginTileData tile) {
        if (tile.getType() == null) {
            return "Custom goal";
        }
        switch (tile.getType()) {
            case SKILL_XP:
                return "Skill XP";
            case ITEM_DROP:
                return "Item drop";
            case KILL_COUNT:
                return "Kill count";
            default:
                return "Custom goal";
        }
    }
}
