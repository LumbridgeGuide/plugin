package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;

/**
 * The expanded view of a single tile: everything the player needs to know to
 * complete it, with a button to go back to the list.
 */
public class TileDetailPanel extends JPanel {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 16;

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;
    private final JPanel content;

    public TileDetailPanel(ItemManager itemManager, SkillIconManager skillIconManager, Runnable onClose) {
        super(new BorderLayout(0, 8));
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
        setOpaque(false);

        JButton closeButton = Components.secondaryButton("‹  Back to tiles");
        closeButton.addActionListener(event -> onClose.run());
        closeButton.setHorizontalAlignment(JButton.LEFT);

        content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(content, BorderLayout.NORTH);

        add(closeButton, BorderLayout.NORTH);
        add(contentWrapper, BorderLayout.CENTER);
    }

    public void show(PluginTileData tile, PluginBoardData board) {
        content.removeAll();

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);
        badges.add(new Badge(TileText.typeName(tile).toUpperCase(Locale.ROOT),
                Theme.SURFACE_OVERLAY, Theme.ACCENT));
        if (board.isTilePointsEnabled() && tile.getPoints() > 0) {
            badges.add(new Badge(tile.getPoints() + (tile.getPoints() == 1 ? " POINT" : " POINTS"),
                    Theme.SURFACE_OVERLAY, Theme.TEXT_SECONDARY));
        }
        addRow(badges, 6);

        addRow(Components.wrapped(tile.getTitle(), Components.CONTENT_WIDTH, 16f, Font.BOLD, Theme.TEXT_PRIMARY), 8);

        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            addRow(Components.wrapped(tile.getDescription(), Components.CONTENT_WIDTH, 12f, Font.PLAIN,
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

        addRow(sectionHeading("Requirements"), 14);

        if (hasSkill) {
            addRow(skillRow(tile), 6);
        }
        if (hasMonster) {
            String kills = tile.getKillCount() != null && tile.getKillCount() > 0
                    ? "Kill " + tile.getMonsterName() + " x" + tile.getKillCount()
                    : "Kill " + tile.getMonsterName();
            addRow(card(Components.wrapped(kills, TEXT_WIDTH, 12f, Font.BOLD, Theme.TEXT_PRIMARY)), 6);
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
        return card(row);
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
        return card(row);
    }

    private void addClaimStatus(PluginTileData tile, PluginBoardData board) {
        addRow(sectionHeading("Status"), 14);

        if (!tile.isClaimed()) {
            addRow(card(Components.label("Not claimed yet", 12f, Font.BOLD, Theme.TEXT_MUTED)), 6);
            return;
        }

        PluginTeamData team = board.getMyTeam();
        boolean mine = team != null && team.getId() != null && team.getId().equals(tile.getClaimedByTeamId());
        if (mine) {
            Color teamColor = Theme.parseTeamColor(team.getColor());
            addRow(card(Components.label("Claimed by " + team.getName(), 12f, Font.BOLD, teamColor)), 6);
        } else {
            addRow(card(Components.label("Claimed by another team", 12f, Font.BOLD, Theme.SUCCESS)), 6);
        }
    }

    private static JLabel sectionHeading(String text) {
        return Components.label(text.toUpperCase(Locale.ROOT), 10f, Font.BOLD, Theme.TEXT_MUTED);
    }

    private static JPanel card(java.awt.Component child) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Theme.SURFACE_RAISED);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(8, 8, 8, 8)));
        card.add(child, BorderLayout.CENTER);
        return card;
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
