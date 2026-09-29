package com.lumbridgeguide.ui;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** The box of a checkbox: an empty inset square, or an accent square with a check mark when selected. */
public class CheckBoxIcon implements Icon {

    private static final int SIZE = 15;
    private static final int ARC = Theme.ARC;

    private final boolean selected;

    public CheckBoxIcon(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (selected) {
            canvas.setColor(Theme.ACCENT);
            canvas.fillRoundRect(x, y, SIZE, SIZE, ARC, ARC);
            new CheckIcon(Theme.TEXT_INVERSE).paintIcon(component, canvas, x, y);
        } else {
            canvas.setColor(Theme.SURFACE_INSET);
            canvas.fillRoundRect(x, y, SIZE, SIZE, ARC, ARC);
            canvas.setColor(Theme.BORDER);
            canvas.drawRoundRect(x, y, SIZE - 1, SIZE - 1, ARC, ARC);
        }
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
