package com.lumbridgeguide.bingo;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

import static org.junit.Assert.assertEquals;

public class TileProgressCounterTest {

    private final Map<String, Map<String, Long>> saved = new HashMap<>();
    private final List<String> reached = new ArrayList<>();
    private TileProgressCounter counter;

    @Before
    public void setUp() {
        counter = newSession();
    }

    @Test
    public void countsBossKillsFromKillCountMessagesAndReachesTheTarget() {
        PluginBoardData board = board(killTile("vork", "Vorkath", null, 3));

        chat(board, "Your <col=ff0000>Vorkath</col> kill count is: <col=ff0000>1,032</col>.");
        chat(board, "Your Vorkath kill count is: 1,033.");
        assertEquals(OptionalLong.of(2), counter.progressFor("vork"));
        assertEquals(List.of(), reached);

        chat(board, "Your Vorkath kill count is: 1,034.");
        assertEquals(OptionalLong.of(3), counter.progressFor("vork"));
        assertEquals(List.of("vork"), reached);
    }

    @Test
    public void countsOrdinaryMonstersFromTheirLootForgivingHowTheHostTypedTheName() {
        PluginBoardData board = board(killTile("demons", "abyssal demons", null, 2));

        counter.lootReceived(List.of(board), "Abyssal demon", 415);
        counter.lootReceived(List.of(board), "Goblin", 3029);
        counter.lootReceived(List.of(board), "Abyssal demon", 415);

        assertEquals(OptionalLong.of(2), counter.progressFor("demons"));
        assertEquals(List.of("demons"), reached);
    }

    @Test
    public void aBossKillWithLootAndMessageCountsOnceInEitherOrder() {
        PluginBoardData board = board(killTile("vork", "Vorkath", null, 5));

        counter.lootReceived(List.of(board), "Vorkath", 8061);
        chat(board, "Your Vorkath kill count is: 50.");
        assertEquals(OptionalLong.of(1), counter.progressFor("vork"));

        chat(board, "Your Vorkath kill count is: 51.");
        counter.lootReceived(List.of(board), "Vorkath", 8061);
        assertEquals(OptionalLong.of(2), counter.progressFor("vork"));
    }

    @Test
    public void progressSurvivesARestart() {
        PluginBoardData board = board(killTile("vork", "Vorkath", null, 5), killTile("demons", "Abyssal demon", null, 5));
        chat(board, "Your Vorkath kill count is: 50.");
        counter.lootReceived(List.of(board), "Abyssal demon", 415);

        counter = newSession();
        chat(board, "Your Vorkath kill count is: 52.");
        counter.lootReceived(List.of(board), "Abyssal demon", 415);

        assertEquals(OptionalLong.of(3), counter.progressFor("vork"));
        assertEquals(OptionalLong.of(2), counter.progressFor("demons"));
    }

    private TileProgressCounter newSession() {
        TileProgressCounter fresh = new TileProgressCounter(new BaselineStore() {
            @Override
            public Map<String, Long> load(String boardId) {
                return new HashMap<>(saved.getOrDefault(boardId, Map.of()));
            }

            @Override
            public void save(String boardId, Map<String, Long> baselines) {
                saved.put(boardId, new HashMap<>(baselines));
            }
        });
        fresh.setOnTargetReached((boardAndTile, detection) -> reached.add(boardAndTile.getValue().getId()));
        return fresh;
    }

    private void chat(PluginBoardData board, String message) {
        KillCount.parse(message).ifPresent(kc -> counter.killCountSeen(List.of(board), kc));
    }

    private static PluginBoardData board(PluginTileData... tiles) {
        PluginBoardData board = new PluginBoardData();
        board.setId("board");
        board.setTiles(List.of(tiles));
        return board;
    }

    private static PluginTileData killTile(String id, String monster, Integer monsterId, int target) {
        PluginTileData tile = new PluginTileData();
        tile.setId(id);
        tile.setType(TileType.KILL_COUNT);
        tile.setMonsterName(monster);
        tile.setMonsterId(monsterId);
        tile.setKillCount(target);
        return tile;
    }
}
