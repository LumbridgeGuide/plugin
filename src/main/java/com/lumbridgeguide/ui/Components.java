package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Small factory for themed Swing components. Every component gets an explicit
 * font because RuneLite's global pixel font would otherwise leak in.
 */
public final class Components {

    public static final int CONTENT_WIDTH = 188;

    public static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(FontManager.getRunescapeFont());
        label.setForeground(color);
        return label;
    }

    /**
     * Text that wraps at the given pixel width.
     */
    public static WrapText wrapped(String text, int width, float size, int style, Color color) {
        WrapText wrapText = new WrapText(width);
        wrapText.setFont(FontManager.getRunescapeFont());
        wrapText.setForeground(color);
        wrapText.setText(text);
        return wrapText;
    }

    public static JButton button(String text, Color background, Color foreground, Color hover) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeFont());
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(7, 12, 7, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                button.setBackground(background);
            }
        });
        return button;
    }

    public static JButton primaryButton(String text) {
        return button(text, Theme.ACCENT, Theme.TEXT_INVERSE, Theme.ACCENT_HOVER);
    }

    public static JButton secondaryButton(String text) {
        return button(text, Theme.SURFACE_OVERLAY, Theme.TEXT_PRIMARY, Theme.BORDER);
    }

    private Components() {
    }
}
