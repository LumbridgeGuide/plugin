package com.lumbridgeguide.ui;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

/** The notification bell for the Inbox button, drawn as an outline in the secondary text colour. */
public class BellIcon implements Icon {

    private static final int SIZE = 14;

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.translate(x, y);
        canvas.setColor(Theme.TEXT_SECONDARY);
        canvas.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D bell = new Path2D.Float();
        bell.moveTo(3.5, 10);
        bell.curveTo(4.5, 9, 4, 7.5, 4, 5.5);
        bell.curveTo(4, 3.3, 5.3, 1.8, 7, 1.8);
        bell.curveTo(8.7, 1.8, 10, 3.3, 10, 5.5);
        bell.curveTo(10, 7.5, 9.5, 9, 10.5, 10);
        bell.closePath();
        canvas.draw(bell);
        canvas.drawLine(6, 12, 8, 12);
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
