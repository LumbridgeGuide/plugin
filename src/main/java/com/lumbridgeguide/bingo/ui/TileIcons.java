package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileType;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;

import java.awt.image.BufferedImage;

@RequiredArgsConstructor
public class TileIcons {

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;

    public BufferedImage imageFor(PluginTileData tile) {
        if (tile.getType() == TileType.ITEM_DROP && tile.getItems() != null && !tile.getItems().isEmpty()) {
            return itemManager.getImage(tile.getItems().get(0).getId());
        }
        if (tile.getType() == TileType.SKILL_XP) {
            return tile.skill().map(skill -> skillIconManager.getSkillImage(skill, false)).orElse(null);
        }
        if (tile.getType() == TileType.KILL_COUNT) {
            return skillIconManager.getSkillImage(Skill.SLAYER, false);
        }
        return null;
    }

    public BufferedImage itemImage(int itemId) {
        return itemManager.getImage(itemId);
    }
}
