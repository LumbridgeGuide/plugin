package com.lumbridgeguide.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A small label: a tile type chip such as DROP or KC, or a status such as LINKED. The outline is optional.
 */
public class Badge extends JComponent {

    private final String text;
    private final Color background;
    private final Color foreground;
    private final Color outline;
    private final Font font = Theme.monoFont(Font.BOLD, 10f);

    public Badge(String text, Color background, Color foreground) {
        this(text, background, foreground, null);
    }

    public Badge(String text, Color background, Color foreground, Color outline) {
        this.text = text;
        this.background = background;
        this.foreground = foreground;
        this.outline = outline;
    }

    /** The inset chip that names a tile's type. */
    public static Badge chip(String text) {
        return new Badge(text, Theme.SURFACE_INSET, Theme.TEXT_SECONDARY, Theme.BORDER_SUBTLE);
    }

    /** A status in its tone's colour on a faint wash of the same colour. */
    public static Badge status(String text, Color tone) {
        return new Badge(text, new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 40), tone);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(font);
        return new Dimension(metrics.stringWidth(text) + 12, metrics.getHeight() + 4);
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
        canvas.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.ARC, Theme.ARC);
        if (outline != null) {
            canvas.setColor(outline);
            canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.ARC, Theme.ARC);
        }
        canvas.setFont(font);
        canvas.setColor(foreground);
        FontMetrics metrics = canvas.getFontMetrics();
        int textX = (getWidth() - metrics.stringWidth(text)) / 2;
        int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        canvas.drawString(text, textX, textY);
        canvas.dispose();
    }
}
