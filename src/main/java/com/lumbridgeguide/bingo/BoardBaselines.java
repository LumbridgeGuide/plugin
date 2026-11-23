package com.lumbridgeguide.bingo;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BoardBaselines {

    private static final String LOOT_KEY_PREFIX = "loot:";

    private final BaselineStore store;
    private final String boardId;
    private final Map<String, Long> baselines;
    private boolean changed;

    public static BoardBaselines load(BaselineStore store, String boardId) {
        return new BoardBaselines(store, boardId, new HashMap<>(store.load(boardId)));
    }

    public boolean hasKillCount(String tileId) {
        return baselines.containsKey(tileId);
    }

    public long killCountBaseline(String tileId, long killCount) {
        Long baseline = baselines.get(tileId);
        if (baseline != null) {
            return baseline;
        }

        Long lootKills = baselines.remove(LOOT_KEY_PREFIX + tileId);
        return set(tileId, killCount - Math.max(1, lootKills == null ? 0 : lootKills));
    }

    public long addLootKill(String tileId) {
        changed = true;
        return baselines.merge(LOOT_KEY_PREFIX + tileId, 1L, Long::sum);
    }

    public long xpBaseline(String tileId, long totalXp) {
        Long baseline = baselines.get(tileId);
        return baseline != null ? baseline : set(tileId, totalXp);
    }

    public void saveIfChanged() {
        if (changed) {
            store.save(boardId, baselines);
        }
    }

    private long set(String tileId, long baseline) {
        baselines.put(tileId, baseline);
        changed = true;
        return baseline;
    }
}
