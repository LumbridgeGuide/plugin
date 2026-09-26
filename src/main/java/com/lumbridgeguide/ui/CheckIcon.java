package com.lumbridgeguide.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A check mark drawn as lines, because the panel fonts have no glyph for one.
 */
public class CheckIcon implements Icon {

    private static final int SIZE = 14;

    private final Color color;

    public CheckIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(color);
        canvas.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.drawLine(x + 2, y + 7, x + 6, y + 11);
        canvas.drawLine(x + 6, y + 11, x + 12, y + 3);
        canvas.dispose();
    }

    @Override
    public int getIconWidth() {
        return SIZE;
    }

    @Override
    public int getIconHeight() {
        return SIZE;
    }
}
