package com.lumbridgeguide.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small rounded square in a team's colour. */
public class SwatchIcon implements Icon {

    private static final int SIZE = 9;

    private final Color color;

    public SwatchIcon(Color color) {
        this.color = color;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(color);
        canvas.fillRoundRect(x, y, SIZE, SIZE, 3, 3);
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
