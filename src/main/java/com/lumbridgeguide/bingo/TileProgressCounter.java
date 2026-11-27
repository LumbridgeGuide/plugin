package com.lumbridgeguide.bingo;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.runelite.api.Skill;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class TileProgressCounter {

    private final BaselineStore store;
    private final Map<String, Long> progress = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> changedTilesByBoard = new ConcurrentHashMap<>();

    @Setter
    private BiConsumer<Map.Entry<PluginBoardData, PluginTileData>, Detection> onTargetReached = (tile, seen) -> { };

    public void killCountSeen(List<PluginBoardData> boards, KillCount killCount) {
        Detection detection = Detection.killCount(killCount);
        for (PluginBoardData board : boards) {
            BoardBaselines baselines = BoardBaselines.load(store, board.getId());
            for (PluginTileData tile : board.tiles()) {
                if (!tile.countsKillsOf(killCount.getMonster(), null)) {
                    continue;
                }

                long kills = killCount.getCount() - baselines.killCountBaseline(tile.getId(), killCount.getCount());
                record(board, tile, kills, true, detection);
            }
            baselines.saveIfChanged();
        }
    }

    public void lootReceived(List<PluginBoardData> boards, String npcName, int npcId) {
        Detection detection = Detection.loot(npcId, npcName);
        for (PluginBoardData board : boards) {
            BoardBaselines baselines = BoardBaselines.load(store, board.getId());
            for (PluginTileData tile : board.tiles()) {
                if (!tile.countsKillsOf(npcName, npcId) || baselines.hasKillCount(tile.getId())) {
                    continue;
                }

                record(board, tile, baselines.addLootKill(tile.getId()), true, detection);
            }
            baselines.saveIfChanged();
        }
    }

    public void xpSeen(List<PluginBoardData> boards, Skill skill, long totalXp, boolean mayComplete) {
        Detection detection = Detection.xp();
        for (PluginBoardData board : boards) {
            BoardBaselines baselines = BoardBaselines.load(store, board.getId());
            for (PluginTileData tile : board.tiles()) {
                if (!tile.tracksXpIn(skill)) {
                    continue;
                }

                long gained = totalXp - baselines.xpBaseline(tile.getId(), totalXp);
                record(board, tile, gained, mayComplete, detection);
            }
            baselines.saveIfChanged();
        }
    }

    public Long xpGained(String boardId, String tileId, long totalXp) {
        Long baseline = store.load(boardId).get(tileId);
        return baseline == null ? null : Math.max(0, totalXp - baseline);
    }

    public Long progressFor(String tileId) {
        return progress.get(tileId);
    }

    public void rearm(String tileId) {
        progress.remove(tileId);
    }

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

    public void clearSession() {
        progress.clear();
        changedTilesByBoard.clear();
    }

    private void record(PluginBoardData board, PluginTileData tile, long amount, boolean mayComplete,
                        Detection detection) {
        long value = Math.max(0, amount);
        Long previous = progress.put(tile.getId(), value);
        if (previous == null || previous != value) {
            markChanged(board.getId(), Set.of(tile.getId()));
        }

        long target = tile.target();
        boolean reachedNow = target > 0 && value >= target && (previous == null || previous < target);
        if (mayComplete && reachedNow) {
            onTargetReached.accept(Map.entry(board, tile), detection);
        }
    }
}
