package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginClaimData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginStandingData;
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

    static String claimingTeamName(PluginTileData tile, PluginBoardData board) {
        String teamId = tile.getClaimedByTeamId();
        if (teamId != null && board.getStandings() != null) {
            for (PluginStandingData standing : board.getStandings()) {
                if (teamId.equals(standing.getTeamId())) {
                    return standing.getName();
                }
            }
        }
        if (teamId != null && board.getRecentClaims() != null) {
            for (PluginClaimData claim : board.getRecentClaims()) {
                if (teamId.equals(claim.getTeamId())) {
                    return claim.getTeamName();
                }
            }
        }
        return "another team";
    }

    static String pointsLabel(PluginTileData tile, PluginBoardData board) {
        if (!board.isTilePointsEnabled() || tile.getPoints() <= 0) {
            return null;
        }
        return tile.getPoints() + (tile.getPoints() == 1 ? " point" : " points");
    }

    static long target(PluginTileData tile) {
        if ("kill_count".equals(tile.getType())) {
            return tile.getKillCount() == null ? 0 : tile.getKillCount();
        }
        return "skill_xp".equals(tile.getType()) ? tile.getXpTarget() : 0;
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
        long target = target(tile);
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
