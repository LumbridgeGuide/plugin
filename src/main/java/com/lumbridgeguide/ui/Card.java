package com.lumbridgeguide.ui;

import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A {@link Section} on a raised surface with a hairline border, for a form or a block of details. */
public class Card extends Section {

    public Card(int padding) {
        super(0);
        setBorder(new EmptyBorder(padding, padding, padding, padding));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(Theme.SURFACE_RAISED);
        canvas.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.ARC, Theme.ARC);
        canvas.setColor(Theme.BORDER);
        canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Theme.ARC, Theme.ARC);
        canvas.dispose();
    }
}
