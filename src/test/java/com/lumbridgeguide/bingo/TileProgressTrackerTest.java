package com.lumbridgeguide.bingo;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TileProgressTrackerTest {

    @Test
    public void readsTheMonsterAndCountFromAKillCountMessage() {
        Optional<TileProgressTracker.KillCount> killCount =
                TileProgressTracker.parseKillCount("Your <col=ff0000>Vorkath</col> kill count is: <col=ff0000>1,032</col>.");

        assertEquals(new TileProgressTracker.KillCount("Vorkath", 1032), killCount.orElseThrow());
    }

    @Test
    public void ignoresOtherMessages() {
        assertFalse(TileProgressTracker.parseKillCount("Your reward is: 3 x Shark.").isPresent());
    }
}
