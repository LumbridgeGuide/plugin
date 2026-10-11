package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginClaimData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginStandingData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;

import java.util.Locale;
import java.util.OptionalLong;

/** What the tile list and the tile view both need to know about a tile: who claimed it and how far along it is. */
final class TileState {

    static boolean isMine(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        return tile.isClaimed() && team != null && team.getId() != null
                && team.getId().equals(tile.getClaimedByTeamId());
    }

    /** The name of the team that claimed a tile, from the standings or the recent claims, whichever has it. */
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

    static long target(PluginTileData tile) {
        if ("kill_count".equals(tile.getType())) {
            return tile.getKillCount() == null ? 0 : tile.getKillCount();
        }
        return "skill_xp".equals(tile.getType()) ? tile.getXpTarget() : 0;
    }

    /** The viewer's own count: this session's tracking when there is any, otherwise what the last sync carried. */
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

    /** "11 / 25" for a kill count tile or "412k / 1m" for an XP tile, or nothing for any other tile. */
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

    private TileState() {
    }
}
