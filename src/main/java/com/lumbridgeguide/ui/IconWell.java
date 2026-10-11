package com.lumbridgeguide.ui;

import net.runelite.client.util.AsyncBufferedImage;

import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * A small square that holds a game icon, such as a tile's item or skill: inset and outlined like an empty inventory
 * slot, with the icon scaled to fit. Item images load in the background and appear when they arrive.
 */
public class IconWell extends JComponent {

    private final int size;
    private BufferedImage image;
    private float opacity = 1f;

    public IconWell(int size) {
        this.size = size;
    }

    public void setImage(BufferedImage newImage) {
        image = newImage;
        if (newImage instanceof AsyncBufferedImage) {
            ((AsyncBufferedImage) newImage).onLoaded(this::repaint);
        }
        repaint();
    }

    /** Fades the icon, for a tile another team has already claimed. */
    public void setIconOpacity(float newOpacity) {
        opacity = newOpacity;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(Theme.SURFACE_INSET);
        canvas.fillRoundRect(0, 0, size, size, Theme.ARC, Theme.ARC);
        canvas.setColor(Theme.BORDER);
        canvas.drawRoundRect(0, 0, size - 1, size - 1, Theme.ARC, Theme.ARC);
        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            int room = size - 4;
            double scale = Math.min(1.0, Math.min((double) room / image.getWidth(), (double) room / image.getHeight()));
            int width = (int) Math.round(image.getWidth() * scale);
            int height = (int) Math.round(image.getHeight() * scale);
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            canvas.drawImage(image, (size - width) / 2, (size - height) / 2, width, height, null);
        }
        canvas.dispose();
    }
}
