package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginTileData;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class BingoTilePanel extends JPanel {

    private static final int ARC = 8;
    private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 9);
    private static final Font BADGE_FONT = new Font("SansSerif", Font.BOLD, 8);
    private static final Font POINTS_FONT = new Font("SansSerif", Font.PLAIN, 8);

    private final PluginTileData tileData;
    private boolean hovered;

    public BingoTilePanel(PluginTileData tileData) {
        this.tileData = tileData;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ToolTipManager.sharedInstance().setInitialDelay(200);
        setToolTipText(buildTooltip());

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
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(40, 40);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        canvas.setColor(hovered ? LumbridgeGuideTheme.TILE_HOVER : LumbridgeGuideTheme.TILE_BACKGROUND);
        canvas.fillRoundRect(0, 0, width, height, ARC, ARC);

        if (tileData.isClaimed()) {
            canvas.setColor(LumbridgeGuideTheme.TILE_CLAIMED_OVERLAY);
            canvas.fillRoundRect(0, 0, width, height, ARC, ARC);
        }

        canvas.setColor(hovered ? LumbridgeGuideTheme.BRAND_PRIMARY : LumbridgeGuideTheme.TILE_BORDER);
        canvas.drawRoundRect(0, 0, width - 1, height - 1, ARC, ARC);

        String badge = resolveBadge();
        canvas.setFont(BADGE_FONT);
        FontMetrics badgeMetrics = canvas.getFontMetrics();
        int badgeWidth = badgeMetrics.stringWidth(badge) + 6;
        int badgeHeight = badgeMetrics.getHeight();
        canvas.setColor(LumbridgeGuideTheme.BRAND_ACCENT);
        canvas.fillRoundRect(3, 3, badgeWidth, badgeHeight, 4, 4);
        canvas.setColor(LumbridgeGuideTheme.TEXT_PRIMARY);
        canvas.drawString(badge, 6, 3 + badgeMetrics.getAscent());

        canvas.setFont(TITLE_FONT);
        FontMetrics titleMetrics = canvas.getFontMetrics();
        String truncatedTitle = truncateText(tileData.getTitle(), titleMetrics, width - 8);
        int titleY = 3 + badgeHeight + 2 + titleMetrics.getAscent();
        canvas.setColor(LumbridgeGuideTheme.TEXT_PRIMARY);
        canvas.drawString(truncatedTitle, 4, titleY);

        if (tileData.getPoints() > 0) {
            String pointsText = tileData.getPoints() + "pt";
            canvas.setFont(POINTS_FONT);
            FontMetrics pointsMetrics = canvas.getFontMetrics();
            int pointsWidth = pointsMetrics.stringWidth(pointsText);
            canvas.setColor(LumbridgeGuideTheme.BRAND_PRIMARY);
            canvas.drawString(pointsText, width - pointsWidth - 4, height - 4);
        }

        if (tileData.isClaimed()) {
            canvas.setColor(LumbridgeGuideTheme.STATUS_SUCCESS);
            canvas.setFont(new Font("SansSerif", Font.BOLD, 12));
            canvas.drawString("✓", width - 14, height - 4);
        }

        canvas.dispose();
    }

    private String resolveBadge() {
        if (tileData.getType() == null) {
            return "★";
        }
        switch (tileData.getType()) {
            case "skill_xp":
                return "XP";
            case "item_drop":
                return "DROP";
            case "kill_count":
                return "KC";
            default:
                return "★";
        }
    }

    private String buildTooltip() {
        StringBuilder tooltip = new StringBuilder("<html><b>" + escapeHtml(tileData.getTitle()) + "</b>");

        if (tileData.getDescription() != null && !tileData.getDescription().isEmpty()) {
            tooltip.append("<br>").append(escapeHtml(tileData.getDescription()));
        }

        if (tileData.getPoints() > 0) {
            tooltip.append("<br><b>Points:</b> ").append(tileData.getPoints());
        }

        if (tileData.getSkill() != null) {
            tooltip.append("<br><b>Skill:</b> ").append(tileData.getSkill());
            if (tileData.getXpTarget() > 0) {
                tooltip.append(" (").append(String.format("%,d", tileData.getXpTarget())).append(" XP)");
            }
        }

        if (tileData.getMonsterName() != null) {
            tooltip.append("<br><b>Monster:</b> ").append(escapeHtml(tileData.getMonsterName()));
            if (tileData.getKillCount() != null && tileData.getKillCount() > 0) {
                tooltip.append(" x").append(tileData.getKillCount());
            }
        }

        if (tileData.getItems() != null && !tileData.getItems().isEmpty()) {
            tooltip.append("<br><b>Items:</b> ");
            for (int index = 0; index < tileData.getItems().size(); index++) {
                if (index > 0) {
                    tooltip.append(", ");
                }
                tooltip.append(escapeHtml(tileData.getItems().get(index).getName()));
            }
        }

        if (tileData.isClaimed()) {
            tooltip.append("<br><span style='color:#00BE00'><b>Claimed</b></span>");
        }

        tooltip.append("</html>");
        return tooltip.toString();
    }

    private static String truncateText(String text, FontMetrics metrics, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        for (int length = text.length() - 1; length > 0; length--) {
            if (metrics.stringWidth(text.substring(0, length)) + ellipsisWidth <= maxWidth) {
                return text.substring(0, length) + ellipsis;
            }
        }
        return ellipsis;
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

