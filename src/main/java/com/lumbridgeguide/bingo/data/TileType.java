package com.lumbridgeguide.bingo.data;

import com.google.gson.annotations.SerializedName;

public enum TileType {
    @SerializedName("item_drop")
    ITEM_DROP,
    @SerializedName("skill_xp")
    SKILL_XP,
    @SerializedName("kill_count")
    KILL_COUNT
}
