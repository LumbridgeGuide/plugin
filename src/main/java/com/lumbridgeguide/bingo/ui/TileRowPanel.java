package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One tile in the list: its type chip, title, points and who claimed it, over a hairline rule. The viewer's team's
 * tiles carry a bar in the team's colour down the left.
 */
class TileRowPanel extends JPanel {

    private static final int TITLE_WIDTH = 132;

    TileRowPanel(PluginTileData tile, PluginBoardData board, Runnable onClick) {
        super(new BorderLayout(7, 0));
        setBackground(Theme.SURFACE_RAISED);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean mine = isClaimedByMyTeam(tile, board);
        Color teamColor = mine ? Theme.parseTeamColor(board.getMyTeam().getColor()) : null;
        Border rule = BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE);
        Border bar = mine ? BorderFactory.createMatteBorder(0, 2, 0, 0, teamColor) : new EmptyBorder(0, 0, 0, 0);
        setBorder(BorderFactory.createCompoundBorder(rule, BorderFactory.createCompoundBorder(bar,
                new EmptyBorder(7, mine ? 5 : 2, 7, 2))));

        JPanel chipHolder = new JPanel(new BorderLayout());
        chipHolder.setOpaque(false);
        chipHolder.add(Badge.chip(TileText.badgeLabel(tile)), BorderLayout.NORTH);

        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);

        JComponent title = Components.wrapped(tile.getTitle(), TITLE_WIDTH, 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        textColumn.add(title);

        String summary = summaryText(tile, board, mine);
        if (!summary.isEmpty()) {
            JLabel summaryLabel = Components.label(summary, 10f, Font.PLAIN, mine ? teamColor : Theme.TEXT_MUTED);
            summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
            summaryLabel.setBorder(new EmptyBorder(1, 0, 0, 0));
            textColumn.add(summaryLabel);
        }

        add(chipHolder, BorderLayout.WEST);
        add(textColumn, BorderLayout.CENTER);
        add(Components.label("›", 13f, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                setOpaque(true);
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                setOpaque(false);
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                onClick.run();
            }
        });
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static boolean isClaimedByMyTeam(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        return tile.isClaimed()
                && team != null
                && team.getId() != null
                && team.getId().equals(tile.getClaimedByTeamId());
    }

    private static String summaryText(PluginTileData tile, PluginBoardData board, boolean mine) {
        StringBuilder summary = new StringBuilder();
        if (board.isTilePointsEnabled() && tile.getPoints() > 0) {
            summary.append(tile.getPoints()).append(tile.getPoints() == 1 ? " point" : " points");
        }
        if (tile.isClaimed()) {
            if (summary.length() > 0) {
                summary.append("  ·  ");
            }
            summary.append(mine ? "Your team" : "Claimed");
        }
        return summary.toString();
    }
}
