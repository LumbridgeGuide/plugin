package com.lumbridgeguide.bingo;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Plays real game messages and loot through the counter, the way a player's session would deliver them. */
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
    public void understandsRaidActivityAndChestWordings() {
        PluginBoardData board = board(
                killTile("cox", "Chambers of Xeric", null, 5),
                killTile("wt", "Wintertodt", null, 5),
                killTile("barrows", "Barrows chest", null, 5),
                killTile("toa", "Tombs of Amascut: Expert Mode", null, 5),
                killTile("tob", "Theatre of Blood", null, 5));

        chat(board, "Your completed Chambers of Xeric count is: <col=ff0000>12</col>.");
        chat(board, "Your subdued Wintertodt count is: <col=ff0000>300</col>.");
        chat(board, "Your Barrows chest count is: <col=ff0000>87</col>.");
        chat(board, "Your completion count for Tombs of Amascut: Expert Mode is: <col=ff0000>4</col>.");
        chat(board, "Your completed Theatre of Blood count is: <col=ff0000>9</col>.");

        for (String tile : List.of("cox", "wt", "barrows", "toa", "tob")) {
            assertEquals(tile, OptionalLong.of(1), counter.progressFor(tile));
        }
    }

    @Test
    public void ignoresOtherMessagesAndOtherBosses() {
        PluginBoardData board = board(killTile("vork", "Vorkath", null, 3));

        chat(board, "Your reward is: 3 x Shark.");
        chat(board, "Your Zulrah kill count is: 10.");

        assertFalse(counter.progressFor("vork").isPresent());
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
    public void matchesLootByMonsterIdWhenTheTileHasOne() {
        PluginBoardData board = board(killTile("gob", null, 3029, 5));

        counter.lootReceived(List.of(board), "Goblin", 3029);
        counter.lootReceived(List.of(board), "Goblin", 655);

        assertEquals(OptionalLong.of(1), counter.progressFor("gob"));
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

    @Test
    public void reportsOnlyTilesWhoseProgressChanged() {
        PluginBoardData board = board(killTile("vork", "Vorkath", null, 5));
        chat(board, "Your Vorkath kill count is: 50.");

        assertEquals(Map.of("board", Set.of("vork")), counter.takeChanged());
        assertFalse(counter.hasChanges());
    }

    private TileProgressCounter newSession() {
        TileProgressCounter fresh = new TileProgressCounter(new TileProgressCounter.BaselineStore() {
            @Override
            public Map<String, Long> load(String boardId) {
                return new HashMap<>(saved.getOrDefault(boardId, Map.of()));
            }

            @Override
            public void save(String boardId, Map<String, Long> baselines) {
                saved.put(boardId, new HashMap<>(baselines));
            }
        });
        fresh.setOnTargetReached((board, tile) -> reached.add(tile.getId()));
        return fresh;
    }

    private void chat(PluginBoardData board, String message) {
        TileProgressCounter.parseKillCount(message).ifPresent(kc -> counter.killCountSeen(List.of(board), kc));
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
        tile.setType("kill_count");
        tile.setMonsterName(monster);
        tile.setMonsterId(monsterId);
        tile.setKillCount(target);
        return tile;
    }
}
