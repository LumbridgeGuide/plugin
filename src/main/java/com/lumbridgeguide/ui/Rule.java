package com.lumbridgeguide.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;

/** The hairline in a section heading, filling whatever width the heading leaves it. */
class Rule extends JComponent {

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(8, 1);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 1);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(Theme.BORDER_SUBTLE);
        graphics.fillRect(0, 0, getWidth(), 1);
    }
}
