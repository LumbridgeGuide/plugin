package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.experimental.UtilityClass;

import java.util.Locale;

@UtilityClass
public class TileState {

    public static String claimingTeamName(PluginTileData tile) {
        return tile.getClaimedByTeamName() != null ? tile.getClaimedByTeamName() : "another team";
    }

    public static String pointsLabel(PluginTileData tile, PluginBoardData board) {
        if (!board.isTilePointsEnabled() || tile.getPoints() <= 0) {
            return null;
        }
        return tile.getPoints() + (tile.getPoints() == 1 ? " point" : " points");
    }

    public static long myProgress(PluginTileData tile, Long tracked) {
        if (tracked != null) {
            return tracked;
        }
        if (tile.getProgress() == null) {
            return 0;
        }
        return tile.getProgress().stream().filter(PluginProgressData::isMine)
                .mapToLong(PluginProgressData::getProgress).findFirst().orElse(0);
    }

    public static String progressText(PluginTileData tile, Long tracked) {
        long target = tile.target();
        if (target <= 0) {
            return "";
        }
        return compact(myProgress(tile, tracked)) + " / " + compact(target);
    }

    public static String compact(long value) {
        if (value >= 1_000_000) {
            return trim(value / 1_000_000.0) + "m";
        }
        if (value >= 10_000) {
            return trim(value / 1_000.0) + "k";
        }
        return String.format(Locale.UK, "%,d", value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.UK, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
