package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginTileData;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;

import java.awt.image.BufferedImage;
import java.util.Locale;

class TileIcons {

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;

    TileIcons(ItemManager itemManager, SkillIconManager skillIconManager) {
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
    }

    BufferedImage imageFor(PluginTileData tile) {
        if ("item_drop".equals(tile.getType()) && tile.getItems() != null && !tile.getItems().isEmpty()
                && itemManager != null) {
            return itemManager.getImage(tile.getItems().get(0).getId());
        }
        if (skillIconManager == null) {
            return null;
        }
        if ("skill_xp".equals(tile.getType())) {
            Skill skill = parseSkill(tile.getSkill());
            return skill == null ? null : skillIconManager.getSkillImage(skill, false);
        }
        if ("kill_count".equals(tile.getType())) {
            return skillIconManager.getSkillImage(Skill.SLAYER, false);
        }
        return null;
    }

    BufferedImage itemImage(int itemId) {
        return itemManager == null ? null : itemManager.getImage(itemId);
    }

    static Skill parseSkill(String skillName) {
        if (skillName == null) {
            return null;
        }
        try {
            return Skill.valueOf(skillName.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
