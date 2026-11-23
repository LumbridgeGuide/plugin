package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileType;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;

import java.awt.image.BufferedImage;

class TileIcons {

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;

    TileIcons(ItemManager itemManager, SkillIconManager skillIconManager) {
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
    }

    BufferedImage imageFor(PluginTileData tile) {
        if (tile.getType() == TileType.ITEM_DROP && tile.getItems() != null && !tile.getItems().isEmpty()
                && itemManager != null) {
            return itemManager.getImage(tile.getItems().get(0).getId());
        }
        if (skillIconManager == null) {
            return null;
        }
        if (tile.getType() == TileType.SKILL_XP) {
            return tile.skill().map(skill -> skillIconManager.getSkillImage(skill, false)).orElse(null);
        }
        if (tile.getType() == TileType.KILL_COUNT) {
            return skillIconManager.getSkillImage(Skill.SLAYER, false);
        }
        return null;
    }

    BufferedImage itemImage(int itemId) {
        return itemManager == null ? null : itemManager.getImage(itemId);
    }
}
