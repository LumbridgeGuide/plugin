package com.lumbridgeguide.bingo;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.Value;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TileProgressCounter {

    private static final Pattern KILL_COUNT_MESSAGE = Pattern.compile(
            "Your (?:completion count for |subdued |completed )?(.+?) "
                    + "(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?is: ?([\\d,]+)");
    private static final String LOOT_KEY_PREFIX = "loot:";

    /** One map per board of tile id to baseline, or to loot-counted kills under the loot prefix */
    public interface BaselineStore {
        Map<String, Long> load(String boardId);

        void save(String boardId, Map<String, Long> baselines);
    }

    @Value
    public static class KillCount {
        String monster;
        long count;
    }

    private final BaselineStore store;
    private final Map<String, Long> progress = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> changedTilesByBoard = new ConcurrentHashMap<>();
    private BiConsumer<PluginBoardData, PluginTileData> onTargetReached = (board, tile) -> { };

    public TileProgressCounter(BaselineStore store) {
        this.store = store;
    }

    public void setOnTargetReached(BiConsumer<PluginBoardData, PluginTileData> listener) {
        onTargetReached = listener;
    }

    /** Parses "Your Vorkath kill count is: 32." and its variants, ignoring colour tags, otherwise empty */
    public static Optional<KillCount> parseKillCount(String message) {
        if (message == null) {
            return Optional.empty();
        }
        Matcher matcher = KILL_COUNT_MESSAGE.matcher(message.replaceAll("<[^>]*>", ""));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(new KillCount(matcher.group(1).trim(), Long.parseLong(matcher.group(2).replace(",", ""))));
    }

    /** Sets the matching tiles' progress exactly, and from then on they ignore loot */
    public void killCountSeen(List<PluginBoardData> boards, KillCount killCount) {
        for (PluginBoardData board : boards) {
            Map<String, Long> baselines = store.load(board.getId());
            boolean changed = false;
            for (PluginTileData tile : tiles(board)) {
                if (!isKillCountTile(tile) || !namesMatch(tile.getMonsterName(), killCount.getMonster())) {
                    continue;
                }
                Long baseline = baselines.get(tile.getId());
                if (baseline == null) {
                    Long lootKills = baselines.remove(LOOT_KEY_PREFIX + tile.getId());
                    baseline = killCount.getCount() - Math.max(1, lootKills == null ? 0 : lootKills);
                    baselines.put(tile.getId(), baseline);
                    changed = true;
                }
                update(board, tile, Math.max(0, killCount.getCount() - baseline), true);
            }
            if (changed) {
                store.save(board.getId(), baselines);
            }
        }
    }

    /** Counts one kill on matching tiles that have never seen a kill count message */
    public void lootReceived(List<PluginBoardData> boards, String npcName, int npcId) {
        for (PluginBoardData board : boards) {
            Map<String, Long> baselines = store.load(board.getId());
            boolean changed = false;
            for (PluginTileData tile : tiles(board)) {
                if (!isKillCountTile(tile) || baselines.containsKey(tile.getId()) || !isTheMonster(tile, npcName, npcId)) {
                    continue;
                }
                long kills = baselines.getOrDefault(LOOT_KEY_PREFIX + tile.getId(), 0L) + 1;
                baselines.put(LOOT_KEY_PREFIX + tile.getId(), kills);
                changed = true;
                update(board, tile, kills, true);
            }
            if (changed) {
                store.save(board.getId(), baselines);
            }
        }
    }

    /** The first reading on a board sets the baseline. A reading that may not complete still records progress */
    public void xpSeen(List<PluginBoardData> boards, Skill skill, long totalXp, boolean mayComplete) {
        for (PluginBoardData board : boards) {
            Map<String, Long> baselines = store.load(board.getId());
            boolean changed = false;
            for (PluginTileData tile : tiles(board)) {
                if (!"skill_xp".equals(tile.getType()) || skillOf(tile).filter(skill::equals).isEmpty()) {
                    continue;
                }
                Long baseline = baselines.get(tile.getId());
                if (baseline == null) {
                    baseline = totalXp;
                    baselines.put(tile.getId(), baseline);
                    changed = true;
                }
                update(board, tile, Math.max(0, totalXp - baseline), mayComplete);
            }
            if (changed) {
                store.save(board.getId(), baselines);
            }
        }
    }

    /** XP gained since the tile's baseline, or null when no baseline has been taken */
    public Long xpGained(String boardId, String tileId, long totalXp) {
        Long baseline = store.load(boardId).get(tileId);
        return baseline == null ? null : Math.max(0, totalXp - baseline);
    }

    public OptionalLong progressFor(String tileId) {
        Long value = progress.get(tileId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    /** Forgets that a tile's target was reached, so the next reading at or past it completes it again */
    public void rearm(String tileId) {
        progress.remove(tileId);
    }

    /** Tiles whose progress changed since the last call, by board */
    public Map<String, Set<String>> takeChanged() {
        Map<String, Set<String>> taken = new HashMap<>(changedTilesByBoard);
        changedTilesByBoard.keySet().removeAll(taken.keySet());
        return taken;
    }

    public void markChanged(String boardId, Set<String> tileIds) {
        changedTilesByBoard.computeIfAbsent(boardId, id -> ConcurrentHashMap.newKeySet()).addAll(tileIds);
    }

    public boolean hasChanges() {
        return !changedTilesByBoard.isEmpty();
    }

    /** Forgets this session's progress, baselines stay in the store */
    public void clearSession() {
        progress.clear();
        changedTilesByBoard.clear();
    }

    private void update(PluginBoardData board, PluginTileData tile, long value, boolean mayComplete) {
        Long previous = progress.put(tile.getId(), value);
        if (previous == null || previous != value) {
            markChanged(board.getId(), Set.of(tile.getId()));
        }
        long target = "kill_count".equals(tile.getType())
                ? (tile.getKillCount() == null ? 0 : tile.getKillCount())
                : tile.getXpTarget();
        if (mayComplete && target > 0 && value >= target && (previous == null || previous < target)) {
            onTargetReached.accept(board, tile);
        }
    }

    private static boolean isKillCountTile(PluginTileData tile) {
        return "kill_count".equals(tile.getType()) && (tile.getMonsterName() != null || tile.getMonsterId() != null);
    }

    private static boolean isTheMonster(PluginTileData tile, String npcName, int npcId) {
        if (tile.getMonsterId() != null && tile.getMonsterId() == npcId) {
            return true;
        }
        return namesMatch(tile.getMonsterName(), npcName);
    }

    /** Ignores case, apostrophes, spacing and a plural "s" */
    static boolean namesMatch(String tileMonster, String gameName) {
        if (tileMonster == null || gameName == null) {
            return false;
        }
        String typed = normalise(tileMonster);
        String game = normalise(gameName);
        return !typed.isEmpty() && (typed.equals(game) || typed.equals(game + "s") || game.equals(typed + "s"));
    }

    private static String normalise(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static List<PluginTileData> tiles(PluginBoardData board) {
        return board.getTiles() == null ? List.of() : board.getTiles();
    }

    static Optional<Skill> skillOf(PluginTileData tile) {
        if (tile.getSkill() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Skill.valueOf(tile.getSkill().toUpperCase(Locale.ROOT).replace(' ', '_')));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
