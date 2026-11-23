package com.lumbridgeguide.bingo.data;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PluginTileDataTest {

    private final Gson gson = new Gson();

    @Test
    public void readsTheBackendTileTypes() {
        assertEquals(TileType.ITEM_DROP, gson.fromJson("{\"type\":\"item_drop\"}", PluginTileData.class).getType());
        assertEquals(TileType.SKILL_XP, gson.fromJson("{\"type\":\"skill_xp\"}", PluginTileData.class).getType());
        assertEquals(TileType.KILL_COUNT, gson.fromJson("{\"type\":\"kill_count\"}", PluginTileData.class).getType());
    }

    @Test
    public void treatsAnUnknownTileTypeAsACustomGoal() {
        assertNull(gson.fromJson("{\"type\":\"custom\"}", PluginTileData.class).getType());
    }
}
