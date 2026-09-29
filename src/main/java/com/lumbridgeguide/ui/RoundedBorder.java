package com.lumbridgeguide.ui;

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/** A rounded, filled outline for components that paint their own content inside it, such as text fields. */
public class RoundedBorder extends AbstractBorder {

    private final Color outline;
    private final Color fill;

    public RoundedBorder(Color outline, Color fill) {
        this.outline = outline;
        this.fill = fill;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(fill);
        canvas.fillRoundRect(x, y, width, height, Theme.ARC, Theme.ARC);
        canvas.setColor(outline);
        canvas.drawRoundRect(x, y, width - 1, height - 1, Theme.ARC, Theme.ARC);
        canvas.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(1, 1, 1, 1);
    }
}
