package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.experimental.UtilityClass;

import java.util.Locale;
import java.util.OptionalLong;

@UtilityClass
class TileState {

    static boolean isMine(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        return tile.isClaimed() && team != null && team.getId() != null
                && team.getId().equals(tile.getClaimedByTeamId());
    }

    static String claimingTeamName(PluginTileData tile) {
        return tile.getClaimedByTeamName() != null ? tile.getClaimedByTeamName() : "another team";
    }

    static String pointsLabel(PluginTileData tile, PluginBoardData board) {
        if (!board.isTilePointsEnabled() || tile.getPoints() <= 0) {
            return null;
        }
        return tile.getPoints() + (tile.getPoints() == 1 ? " point" : " points");
    }

    static long myProgress(PluginTileData tile, OptionalLong tracked) {
        if (tracked.isPresent()) {
            return tracked.getAsLong();
        }
        if (tile.getProgress() == null) {
            return 0;
        }
        return tile.getProgress().stream().filter(PluginProgressData::isMine)
                .mapToLong(PluginProgressData::getProgress).findFirst().orElse(0);
    }

    static String progressText(PluginTileData tile, OptionalLong tracked) {
        long target = tile.target();
        if (target <= 0) {
            return "";
        }
        return compact(myProgress(tile, tracked)) + " / " + compact(target);
    }

    static String compact(long value) {
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
