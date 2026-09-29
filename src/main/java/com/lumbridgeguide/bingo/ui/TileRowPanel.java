package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One tile in the list: its points as a big number, then its title over a line saying who claimed it or what kind
 * of tile it is. The number is in the viewer's team colour once they claim it, and fades once another team does.
 */
class TileRowPanel extends JPanel {

    private static final int POINTS_WIDTH = 22;
    private static final int TITLE_WIDTH = 150;

    TileRowPanel(PluginTileData tile, PluginBoardData board, Runnable onClick) {
        super(new BorderLayout(8, 0));
        setBackground(Theme.SURFACE_RAISED);
        setOpaque(false);
        setBorder(new EmptyBorder(7, 2, 7, 2));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean mine = isClaimedByMyTeam(tile, board);
        boolean claimedByOthers = tile.isClaimed() && !mine;
        Color teamColor = mine ? Theme.parseTeamColor(board.getMyTeam().getColor()) : null;

        if (board.isTilePointsEnabled()) {
            Color pointsColor = mine ? teamColor : claimedByOthers ? Theme.TEXT_MUTED : Theme.ACCENT;
            JLabel points = Components.label(String.valueOf(tile.getPoints()), 16f, Font.BOLD, pointsColor);
            points.setVerticalAlignment(JLabel.TOP);
            points.setPreferredSize(new Dimension(POINTS_WIDTH, points.getPreferredSize().height));
            add(points, BorderLayout.WEST);
        }

        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);

        JComponent title = Components.wrapped(tile.getTitle(), TITLE_WIDTH, 13f, Font.PLAIN,
                claimedByOthers ? Theme.TEXT_MUTED : Theme.TEXT_PRIMARY);
        title.setAlignmentX(LEFT_ALIGNMENT);
        textColumn.add(title);

        String summary = mine ? "Your team" : claimedByOthers ? "Claimed" : TileText.typeName(tile);
        JLabel summaryLabel = Components.label(summary, 10f, Font.PLAIN, mine ? teamColor : Theme.TEXT_MUTED);
        summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
        summaryLabel.setBorder(new EmptyBorder(1, 0, 0, 0));
        textColumn.add(summaryLabel);

        add(textColumn, BorderLayout.CENTER);

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
}
