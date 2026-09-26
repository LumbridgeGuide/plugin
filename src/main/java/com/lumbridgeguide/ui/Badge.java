package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A small rounded pill with centred text, used for tile types and team names.
 */
public class Badge extends JComponent {

    private final String text;
    private final Color background;
    private final Color foreground;
    private final Font font = FontManager.getRunescapeFont();

    public Badge(String text, Color background, Color foreground) {
        this.text = text;
        this.background = background;
        this.foreground = foreground;
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(font);
        return new Dimension(metrics.stringWidth(text) + 14, metrics.getHeight() + 4);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setColor(background);
        canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        canvas.setFont(font);
        canvas.setColor(foreground);
        FontMetrics metrics = canvas.getFontMetrics();
        int textX = (getWidth() - metrics.stringWidth(text)) / 2;
        int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        canvas.drawString(text, textX, textY);
        canvas.dispose();
    }
}
