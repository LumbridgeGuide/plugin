package com.lumbridgeguide.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A thin bar filled to a fraction in a given colour, such as a team's share of claimed tiles. */
public class ProgressBar extends JComponent {

    private final int height;
    private double fraction;
    private Color color;

    public ProgressBar() {
        this(4);
    }

    public ProgressBar(int height) {
        this.height = height;
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public void setProgress(double newFraction, Color newColor) {
        fraction = Math.max(0, Math.min(1, newFraction));
        color = newColor;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Components.CONTENT_WIDTH, height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, height);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(Theme.BORDER_SUBTLE);
        canvas.fillRoundRect(0, 0, getWidth(), height, Theme.ARC, Theme.ARC);
        int filled = (int) Math.round(getWidth() * fraction);
        if (filled > 0 && color != null) {
            canvas.setColor(color);
            canvas.fillRoundRect(0, 0, filled, height, Theme.ARC, Theme.ARC);
        }
        canvas.dispose();
    }
}
