package com.lumbridgeguide.ui;

import javax.swing.border.AbstractBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * A rounded hairline outline. It only draws the outline: a border paints after its component, so a fill here would
 * cover the component's own content, such as the text in a field.
 */
public class RoundedBorder extends AbstractBorder {

    private final Color outline;

    public RoundedBorder(Color outline) {
        this.outline = outline;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(outline);
        canvas.drawRoundRect(x, y, width - 1, height - 1, Theme.ARC, Theme.ARC);
        canvas.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(1, 1, 1, 1);
    }
}
