package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.Theme;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.function.BiConsumer;

public class TileDetailPanel extends JPanel {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 24;

    private final ItemManager itemManager;
    private final SkillIconManager skillIconManager;
    private final TileProgressTracker progressTracker;
    private final BiConsumer<PluginTileData, PluginBoardData> onSendProof;
    private final Card content;

    public TileDetailPanel(ItemManager itemManager, SkillIconManager skillIconManager,
                           TileProgressTracker progressTracker, Runnable onClose,
                           BiConsumer<PluginTileData, PluginBoardData> onSendProof) {
        super(new BorderLayout(0, 8));
        this.itemManager = itemManager;
        this.skillIconManager = skillIconManager;
        this.progressTracker = progressTracker;
        this.onSendProof = onSendProof;
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
                    11f, Font.BOLD, Theme.accent());
            points.setFont(Theme.monoFont(Font.BOLD, 10f));
            addRow(points, 4);
        }

        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            addRow(Components.wrapped(tile.getDescription(), TEXT_WIDTH, 12f, Font.PLAIN,
                    Theme.TEXT_SECONDARY), 8);
        }

        addProgress(tile);
        addRequirements(tile);
        addClaimStatus(tile, board);
        addSendProof(tile, board);

        revalidate();
        repaint();
    }

    /**
     * Kill count and XP tiles show the player's own count, from this session's tracking when there is one or from
     * the last sync otherwise, and their team's split.
     */
    private void addProgress(PluginTileData tile) {
        boolean killCount = "kill_count".equals(tile.getType());
        boolean experience = "skill_xp".equals(tile.getType());
        if (!killCount && !experience) {
            return;
        }
        long target = killCount ? (tile.getKillCount() == null ? 0 : tile.getKillCount()) : tile.getXpTarget();
        List<PluginProgressData> team = tile.getProgress() == null ? List.of() : tile.getProgress();
        OptionalLong tracked = progressTracker == null ? OptionalLong.empty() : progressTracker.progressFor(tile.getId());
        long mine = tracked.orElse(team.stream().filter(PluginProgressData::isMine)
                .mapToLong(PluginProgressData::getProgress).findFirst().orElse(0));
        boolean tracking = progressTracker != null && progressTracker.isTracking();

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(Components.sectionLabel("Your progress"), BorderLayout.WEST);
        if (tracking) {
            JLabel live = Components.label("Tracking", 10f, Font.PLAIN, Theme.SUCCESS);
            live.setIcon(new DotIcon(Theme.SUCCESS));
            live.setIconTextGap(4);
            heading.add(live, BorderLayout.EAST);
        }
        addRow(heading, 14);

        JPanel count = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ((FlowLayout) count.getLayout()).setAlignOnBaseline(true);
        count.setOpaque(false);
        count.setBorder(new EmptyBorder(0, -5, 0, 0));
        count.add(Components.monoLabel(String.format("%,d", mine), 22f, Font.BOLD, Theme.TEXT_PRIMARY));
        String unit = killCount ? " kills" : " XP";
        count.add(Components.label("/ " + String.format("%,d", target) + unit, 11f, Font.PLAIN,
                Theme.TEXT_SECONDARY));
        addRow(count, 4);

        ProgressBar bar = new ProgressBar();
        bar.setProgress(target <= 0 ? 0 : (double) mine / target, Theme.accent());
        addRow(bar, 6);
        addRow(Components.wrapped(tracking
                        ? "Counted from when the plugin first saw this board running, so nothing to type in."
                        : "Turn on Track tile progress in the plugin settings to count this automatically.",
                TEXT_WIDTH, 10f, Font.PLAIN, Theme.TEXT_MUTED), 6);

        if (team.size() > 1) {
            addRow(Components.sectionLabel("Team progress"), 12);
            for (PluginProgressData teammate : team) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.add(Components.label(teammate.isMine() ? "You" : teammate.getName(), 12f, Font.PLAIN,
                        Theme.TEXT_PRIMARY), BorderLayout.WEST);
                row.add(Components.monoLabel(String.format("%,d", teammate.isMine() ? mine : teammate.getProgress()),
                        11f, Font.PLAIN, teammate.isMine() ? Theme.accent() : Theme.TEXT_SECONDARY), BorderLayout.EAST);
                addRow(row, 5);
            }
        }
    }

    /**
     * Any tile the viewer's team has not claimed can have proof sent by hand, from a screenshot taken now. A tile the
     * team has claimed links to its accepted proof on the website instead.
     */
    private void addSendProof(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        boolean mine = team != null && tile.isClaimed() && team.getId().equals(tile.getClaimedByTeamId());
        if (mine && board.getWebUrl() != null && !board.getWebUrl().isEmpty()) {
            JButton openProof = Components.linkButton("Open proof on the website  ›");
            openProof.addActionListener(event -> LinkBrowser.browse(board.getWebUrl() + "?tile=" + tile.getId()));
            addRow(openProof, 14);
            return;
        }
        if (team == null || mine || board.getVerificationCode() == null) {
            return;
        }
        JButton sendProof = Components.linkButton("Send proof from a screenshot  ›");
        sendProof.addActionListener(event -> onSendProof.accept(tile, board));
        addRow(sendProof, 14);
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
