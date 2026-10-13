package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginProgressData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.bingo.data.TileItemEntry;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.IconWell;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.SwatchIcon;
import com.lumbridgeguide.ui.Theme;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.OptionalLong;
import java.util.function.BiConsumer;

/**
 * One tile, as the website's tile view shows it: the tile's icon, title and badges, the player's progress on kill
 * count and XP tiles, what it asks for, who claimed it and the player's proof, then what can be done about it.
 */
public class TileDetailPanel extends JPanel {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH;

    private final TileIcons icons;
    private final TileProgressTracker progressTracker;
    private final BiConsumer<PluginTileData, PluginBoardData> onSendProof;
    private final Section content;

    TileDetailPanel(TileIcons icons, TileProgressTracker progressTracker, Runnable onClose,
                    BiConsumer<PluginTileData, PluginBoardData> onSendProof) {
        super(new BorderLayout(0, 10));
        this.icons = icons;
        this.progressTracker = progressTracker;
        this.onSendProof = onSendProof;
        setOpaque(false);

        JButton closeButton = Components.linkButton("‹  Tiles");
        closeButton.setForeground(Theme.TEXT_SECONDARY);
        closeButton.setFont(Components.font(12f, Font.PLAIN));
        closeButton.addActionListener(event -> onClose.run());

        content = new Section(0);

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setOpaque(false);
        contentWrapper.add(content, BorderLayout.NORTH);

        add(closeButton, BorderLayout.NORTH);
        add(contentWrapper, BorderLayout.CENTER);
    }

    public void show(PluginTileData tile, PluginBoardData board) {
        content.removeAll();

        addHeader(tile, board);
        if (tile.getDescription() != null && !tile.getDescription().isEmpty()) {
            addRow(Components.wrapped(tile.getDescription(), TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY), 10);
        }
        addProgress(tile);
        addGoal(tile);
        addClaimedBy(tile, board);
        addProof(tile, board);
        addActions(tile, board);

        revalidate();
        repaint();
    }

    private void addHeader(PluginTileData tile, PluginBoardData board) {
        IconWell icon = new IconWell(40);
        icon.setImage(icons.imageFor(tile));
        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(icon, BorderLayout.NORTH);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JComponent title = Components.wrapped(tile.getTitle(), TEXT_WIDTH - 50, 14f, Font.BOLD, Theme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        text.add(title);
        text.add(Box.createVerticalStrut(5));
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        badges.setOpaque(false);
        badges.setBorder(new EmptyBorder(0, -4, 0, 0));
        badges.setAlignmentX(LEFT_ALIGNMENT);
        badges.add(Badge.neutral(TileText.typeName(tile)));
        String points = TileState.pointsLabel(tile, board);
        if (points != null) {
            badges.add(Badge.accent(points));
        }
        text.add(badges);

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.add(iconHolder, BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        addRow(header, 0);
    }

    /**
     * Kill count and XP tiles show the player's own count, from this session's tracking when there is one or from
     * the last sync otherwise, and their team's split.
     */
    private void addProgress(PluginTileData tile) {
        long target = TileState.target(tile);
        if (target <= 0) {
            return;
        }
        boolean killCount = "kill_count".equals(tile.getType());
        List<PluginProgressData> team = tile.getProgress() == null ? List.of() : tile.getProgress();
        OptionalLong tracked = progressTracker == null ? OptionalLong.empty() : progressTracker.progressFor(tile.getId());
        long mine = TileState.myProgress(tile, tracked);
        boolean tracking = progressTracker != null && progressTracker.isTracking();

        addRow(Components.sectionHeader("Your progress"), 16);

        JPanel count = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        ((FlowLayout) count.getLayout()).setAlignOnBaseline(true);
        count.setOpaque(false);
        count.setBorder(new EmptyBorder(0, -6, 0, 0));
        count.add(Components.monoLabel(String.format("%,d", mine), 22f, Font.BOLD, Theme.TEXT_PRIMARY));
        count.add(Components.label("of " + String.format("%,d", target) + (killCount ? " kills" : " XP"), 12f,
                Font.PLAIN, Theme.TEXT_SECONDARY));
        addRow(count, 8);

        ProgressBar bar = new ProgressBar();
        bar.setProgress((double) mine / target, Theme.accent());
        addRow(bar, 6);
        if (!tracking) {
            addRow(Components.wrapped("Turn on Track tile progress in the plugin settings to count this.",
                    TEXT_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED), 6);
        }

        if (team.size() > 1) {
            addRow(Components.sectionHeader("Team progress"), 16);
            for (PluginProgressData teammate : team) {
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                row.add(Components.label(teammate.isMine() ? "You" : teammate.getName(), 12f,
                        teammate.isMine() ? Font.BOLD : Font.PLAIN,
                        teammate.isMine() ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY), BorderLayout.WEST);
                row.add(Components.monoLabel(String.format("%,d", teammate.isMine() ? mine : teammate.getProgress()),
                        11f, Font.PLAIN, Theme.TEXT_SECONDARY), BorderLayout.EAST);
                addRow(row, 6);
            }
        }
    }

    /**
     * The items a drop tile accepts, with their icons. Kill count and XP tiles already say what they need in their
     * title and progress, so they have no item list.
     */
    private void addGoal(PluginTileData tile) {
        if (tile.getItems() == null || tile.getItems().isEmpty()) {
            return;
        }
        addRow(Components.sectionHeader(tile.getItems().size() == 1 ? "Item" : "Any of these"), 16);
        for (TileItemEntry item : tile.getItems()) {
            addRow(iconRow(icons.itemImage(item.getId()), item.getName()), 8);
        }
    }

    private static JPanel iconRow(java.awt.image.BufferedImage image, String text) {
        IconWell icon = new IconWell(24);
        icon.setImage(image);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(icon, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(holder, BorderLayout.WEST);
        JPanel textHolder = new JPanel(new BorderLayout());
        textHolder.setOpaque(false);
        textHolder.setBorder(new EmptyBorder(4, 0, 0, 0));
        textHolder.add(Components.wrapped(text, TEXT_WIDTH - 34, 12f, Font.PLAIN, Theme.TEXT_PRIMARY),
                BorderLayout.NORTH);
        row.add(textHolder, BorderLayout.CENTER);
        return row;
    }

    private void addClaimedBy(PluginTileData tile, PluginBoardData board) {
        if (!tile.isClaimed()) {
            return;
        }
        addRow(Components.sectionHeader("Claimed by"), 16);
        boolean mine = TileState.isMine(tile, board);
        PluginTeamData team = board.getMyTeam();
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, -6, 0, 0));
        JLabel name = Components.label(mine ? team.getName() : TileState.claimingTeamName(tile, board), 12f,
                Font.PLAIN, Theme.TEXT_PRIMARY);
        if (mine) {
            name.setIcon(new SwatchIcon(Theme.parseTeamColor(team.getColor())));
            name.setIconTextGap(6);
        }
        row.add(name);
        if (mine) {
            row.add(Badge.status("Your team", Theme.SUCCESS));
        }
        addRow(row, 8);
    }

    /** The player's team's latest proof for the tile, when there is any. */
    private void addProof(PluginTileData tile, PluginBoardData board) {
        String status = tile.getProofStatus();
        if (status == null) {
            return;
        }
        Badge badge;
        switch (status) {
            case "PENDING":
                badge = Badge.status("Waiting for review", Theme.WARNING);
                break;
            case "REJECTED":
                badge = Badge.status("Not accepted", Theme.ERROR);
                break;
            default:
                badge = Badge.status("Approved", Theme.SUCCESS);
                break;
        }
        addRow(Components.sectionHeader("Your proof"), 16);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(badge);
        addRow(row, 8);
    }

    /**
     * Any tile the viewer's team has not claimed can have proof sent by hand, from a screenshot taken now. Every tile
     * links to the board on the website, where its proof is.
     */
    private void addActions(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        boolean mine = TileState.isMine(tile, board);
        if (team != null && !mine && board.getVerificationCode() != null) {
            JButton sendProof = Components.fullWidthButton("Send proof");
            sendProof.addActionListener(event -> onSendProof.accept(tile, board));
            addRow(sendProof, 18);
        }
        if (board.getWebUrl() != null && !board.getWebUrl().isEmpty()) {
            JButton open = Components.secondaryButton(mine ? "Open proof on the website" : "Open on the website");
            open.setMaximumSize(new Dimension(Integer.MAX_VALUE, open.getPreferredSize().height));
            open.addActionListener(event -> LinkBrowser.browse(board.getWebUrl() + "?tile=" + tile.getId()));
            addRow(open, mine ? 18 : 6);
        }
    }

    private void addRow(JComponent component, int spaceAbove) {
        content.add(Box.createVerticalStrut(spaceAbove));
        component.setAlignmentX(LEFT_ALIGNMENT);
        if (component instanceof JPanel) {
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        }
        content.add(component);
    }
}
