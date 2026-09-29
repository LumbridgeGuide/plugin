package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Theme;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

public class TileDetailPanel extends JPanel {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 24;

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;
    private final Card content;

    public TileDetailPanel(ItemManager itemManager, SkillIconManager skillIconManager, Runnable onClose) {
        super(new BorderLayout(0, 8));
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
        setOpaque(false);

        JButton closeButton = Components.linkButton("‹  Back to tiles");
        closeButton.addActionListener(event -> onClose.run());

        content = new Card(12);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(content, BorderLayout.NORTH);

        add(closeButton, BorderLayout.NORTH);
        add(contentWrapper, BorderLayout.CENTER);
    }

    public void show(PluginTileData tile, PluginBoardData board) {
        content.removeAll();

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);
        badges.add(Badge.chip(TileText.badgeLabel(tile)));
        addRow(badges, 0);

        addRow(Components.wrapped(tile.getTitle(), TEXT_WIDTH, 16f, Font.BOLD, Theme.TEXT_PRIMARY), 8);

        if (board.isTilePointsEnabled() && tile.getPoints() > 0) {
            JLabel points = Components.label(tile.getPoints() + (tile.getPoints() == 1 ? " POINT" : " POINTS"),
                    11f, Font.BOLD, Theme.ACCENT);
            points.setFont(Theme.monoFont(Font.BOLD, 10f));
            addRow(points, 4);
        }

        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            addRow(Components.wrapped(tile.getDescription(), TEXT_WIDTH, 12f, Font.PLAIN,
                    Theme.TEXT_SECONDARY), 8);
        }

        addRequirements(tile);
        addClaimStatus(tile, board);

        revalidate();
        repaint();
    }

    private void addRequirements(PluginTileData tile) {
        boolean hasItems = tile.getItems() != null && !tile.getItems().isEmpty();
        boolean hasSkill = tile.getSkill() != null;
        boolean hasMonster = tile.getMonsterName() != null;
        if (!hasItems && !hasSkill && !hasMonster) {
            return;
        }

        addRow(Components.sectionLabel("Requirements"), 14);

        if (hasSkill) {
            addRow(skillRow(tile), 6);
        }
        if (hasMonster) {
            String kills = tile.getKillCount() != null && tile.getKillCount() > 0
                    ? "Kill " + tile.getMonsterName() + " x" + tile.getKillCount()
                    : "Kill " + tile.getMonsterName();
            addRow(Components.wrapped(kills, TEXT_WIDTH, 12f, Font.BOLD, Theme.TEXT_PRIMARY), 6);
        }
        if (hasItems) {
            List<TileItemEntry> items = tile.getItems();
            for (TileItemEntry item : items) {
                addRow(itemRow(item), 6);
            }
        }
    }

    private JPanel skillRow(PluginTileData tile) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        Skill skill = parseSkill(tile.getSkill());
        if (skill != null) {
            JLabel icon = new JLabel(new ImageIcon(skillIconManager.getSkillImage(skill, true)));
            row.add(icon, BorderLayout.WEST);
        }

        String xp = tile.getXpTarget() > 0 ? String.format("%,d XP", tile.getXpTarget()) : "";
        String text = (xp.isEmpty() ? "" : xp + " in ") + TileText.capitalise(tile.getSkill());
        row.add(Components.wrapped(text, TEXT_WIDTH - 30, 12f, Font.BOLD, Theme.TEXT_PRIMARY),
                BorderLayout.CENTER);
        return row;
    }

    private JPanel itemRow(TileItemEntry item) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JLabel icon = new JLabel();
        icon.setPreferredSize(new Dimension(36, 32));
        AsyncBufferedImage image = itemManager.getImage(item.getId());
        image.addTo(icon);

        row.add(icon, BorderLayout.WEST);
        row.add(Components.wrapped(item.getName(), TEXT_WIDTH - 46, 12f, Font.BOLD, Theme.TEXT_PRIMARY),
                BorderLayout.CENTER);
        return row;
    }

    private void addClaimStatus(PluginTileData tile, PluginBoardData board) {
        addRow(Components.sectionLabel("Status"), 14);

        if (!tile.isClaimed()) {
            addRow(Components.statusLine("Not claimed yet", Theme.TEXT_MUTED, Theme.TEXT_SECONDARY), 6);
            return;
        }

        PluginTeamData team = board.getMyTeam();
        boolean mine = team != null && team.getId() != null && team.getId().equals(tile.getClaimedByTeamId());
        if (mine) {
            addRow(Components.statusLine("Claimed by " + team.getName(), Theme.parseTeamColor(team.getColor()),
                    Theme.TEXT_PRIMARY), 6);
        } else {
            addRow(Components.statusLine("Claimed by another team", Theme.TEXT_MUTED, Theme.TEXT_PRIMARY), 6);
        }
    }

    private void addRow(javax.swing.JComponent component, int spaceAbove) {
        content.add(Box.createVerticalStrut(spaceAbove));
        component.setAlignmentX(LEFT_ALIGNMENT);
        if (component instanceof JPanel) {
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        }
        content.add(component);
    }

    private static Skill parseSkill(String skillName) {
        try {
            return Skill.valueOf(skillName.toUpperCase(Locale.ROOT).replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
