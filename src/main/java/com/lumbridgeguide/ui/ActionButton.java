package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A themed button with softly rounded corners, painted by hand because Swing's look and feel draws square ones. A
 * primary button is filled with the accent; a secondary one is a quiet outline.
 */
public class ActionButton extends JButton {

    public enum Kind {
        PRIMARY,
        SECONDARY
    }

    private static final float DISABLED_ALPHA = 0.45f;

    private final Kind kind;
    private boolean hovered;

    public ActionButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setFont(FontManager.getRunescapeBoldFont());
        setForeground(kind == Kind.PRIMARY ? Theme.TEXT_INVERSE : Theme.TEXT_PRIMARY);
        setBorder(new EmptyBorder(7, 14, 7, 14));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                hovered = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (!isEnabled()) {
            canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DISABLED_ALPHA));
        }
        boolean active = hovered && isEnabled();
        int arc = Theme.ARC;
        if (kind == Kind.PRIMARY) {
            canvas.setColor(active ? Theme.accentHover() : Theme.accent());
            canvas.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        } else {
            if (active) {
                canvas.setColor(Theme.SURFACE_OVERLAY);
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            }
            canvas.setColor(Theme.BORDER);
            canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        canvas.dispose();
        super.paintComponent(graphics);
    }
}
