package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTeamData;
import com.lumbridgeguide.data.PluginTileData;
import net.runelite.client.ui.FontManager;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * One tile in the board list: type badge, title, and a short claim summary.
 * Clicking it opens the expanded tile view.
 */
class TileRowPanel extends JPanel {

    private static final int ARC = 8;
    private static final int TITLE_WIDTH = 108;

    private final Color stripeColor;
    private boolean hovered;

    TileRowPanel(PluginTileData tile, PluginBoardData board, Runnable onClick) {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 12, 8, 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        boolean claimedByMyTeam = isClaimedByMyTeam(tile, board);
        stripeColor = claimedByMyTeam
                ? LumbridgeGuideTheme.parseTeamColor(board.getMyTeam().getColor())
                : null;

        JPanel badgeHolder = new JPanel(new BorderLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(new Ui.Badge(TileText.badgeLabel(tile), LumbridgeGuideTheme.SURFACE_OVERLAY,
                LumbridgeGuideTheme.ACCENT), BorderLayout.NORTH);

        JPanel textColumn = new JPanel();
        textColumn.setLayout(new BoxLayout(textColumn, BoxLayout.Y_AXIS));
        textColumn.setOpaque(false);

        Color titleColor = tile.isClaimed() ? LumbridgeGuideTheme.TEXT_SECONDARY : LumbridgeGuideTheme.TEXT_PRIMARY;
        JComponent title = Ui.wrapped(tile.getTitle(), TITLE_WIDTH, 12f, Font.BOLD, titleColor);
        title.setAlignmentX(LEFT_ALIGNMENT);
        textColumn.add(title);

        String summary = summaryText(tile, board);
        if (!summary.isEmpty()) {
            JLabel summaryLabel = Ui.label(summary, 11f, Font.PLAIN, tile.isClaimed() ? LumbridgeGuideTheme.TEXT_SECONDARY : LumbridgeGuideTheme.TEXT_MUTED);
            summaryLabel.setAlignmentX(LEFT_ALIGNMENT);
            summaryLabel.setBorder(new EmptyBorder(2, 0, 0, 0));
            textColumn.add(summaryLabel);
        }

        JLabel trailing = new JLabel("", SwingConstants.CENTER);
        if (tile.isClaimed()) {
            trailing.setIcon(new Ui.CheckIcon(stripeColor != null ? stripeColor : LumbridgeGuideTheme.SUCCESS));
        } else {
            trailing.setText("›");
            trailing.setFont(FontManager.getRunescapeFont());
            trailing.setForeground(LumbridgeGuideTheme.TEXT_MUTED);
        }

        add(badgeHolder, BorderLayout.WEST);
        add(textColumn, BorderLayout.CENTER);
        add(trailing, BorderLayout.EAST);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                hovered = false;
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

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = getWidth();
        int height = getHeight();

        canvas.setColor(hovered ? LumbridgeGuideTheme.SURFACE_OVERLAY : LumbridgeGuideTheme.SURFACE_RAISED);
        canvas.fillRoundRect(0, 0, width, height, ARC, ARC);
        canvas.setColor(hovered ? LumbridgeGuideTheme.ACCENT : LumbridgeGuideTheme.BORDER);
        canvas.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC);

        if (stripeColor != null) {
            canvas.setColor(stripeColor);
            canvas.fillRoundRect(0, 0, 5, height, ARC, ARC);
            canvas.fillRect(3, 0, 2, height);
        }
        canvas.dispose();
    }

    private static boolean isClaimedByMyTeam(PluginTileData tile, PluginBoardData board) {
        PluginTeamData team = board.getMyTeam();
        return tile.isClaimed()
                && team != null
                && team.getId() != null
                && team.getId().equals(tile.getClaimedByTeamId());
    }

    private static String summaryText(PluginTileData tile, PluginBoardData board) {
        StringBuilder summary = new StringBuilder();
        if (board.isTilePointsEnabled() && tile.getPoints() > 0) {
            summary.append(tile.getPoints()).append(tile.getPoints() == 1 ? " point" : " points");
        }
        if (tile.isClaimed()) {
            if (summary.length() > 0) {
                summary.append("  ·  ");
            }
            summary.append(isClaimedByMyTeam(tile, board) ? "Your team" : "Claimed");
        }
        return summary.toString();
    }
}
