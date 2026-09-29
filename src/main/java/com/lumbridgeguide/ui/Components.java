package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Locale;

/**
 * Small factory for themed Swing components. Every component gets an explicit
 * font because RuneLite's global pixel font would otherwise leak in.
 */
public final class Components {

    public static final int CONTENT_WIDTH = 188;

    /** RuneLite's fonts come in three cuts, so a bold style or a small size picks the matching one. */
    public static Font font(float size, int style) {
        if (style == Font.BOLD) {
            return FontManager.getRunescapeBoldFont();
        }
        if (size <= 11f) {
            return FontManager.getRunescapeSmallFont();
        }
        return FontManager.getRunescapeFont();
    }

    public static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(size, style));
        label.setForeground(color);
        return label;
    }

    /** A small mono heading over a group of controls, such as "TILES" or "VERIFICATION CODE". */
    public static JLabel sectionLabel(String text) {
        String upper = text.toUpperCase(Locale.ROOT);
        int tracking = (int) Math.ceil(upper.length() * Theme.LABEL_TRACKING * Theme.LABEL_SIZE) + 1;
        JLabel label = new JLabel(upper) {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(size.width + tracking, size.height);
            }

            @Override
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
        };
        label.setFont(Theme.labelFont());
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * A label in the mono font. Mono glyphs can draw a little wider than Swing measures them on scaled displays, so
     * the label keeps a few pixels of room on the right.
     */
    public static JLabel monoLabel(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.monoFont(style, size));
        label.setForeground(color);
        label.setBorder(new EmptyBorder(0, 0, 0, 4));
        return label;
    }

    /** A line of text led by a coloured dot, for statuses such as "Not claimed yet". */
    public static JLabel statusLine(String text, Color dotColor, Color textColor) {
        JLabel label = label(text, 12f, Font.PLAIN, textColor);
        label.setIcon(new DotIcon(dotColor));
        label.setIconTextGap(6);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    public static WrapText wrapped(String text, int width, float size, int style, Color color) {
        WrapText wrapText = new WrapText(width);
        wrapText.setFont(font(size, style));
        wrapText.setForeground(color);
        wrapText.setText(text);
        return wrapText;
    }

    public static JButton primaryButton(String text) {
        return new ActionButton(text, ActionButton.Kind.PRIMARY);
    }

    public static JButton secondaryButton(String text) {
        return new ActionButton(text, ActionButton.Kind.SECONDARY);
    }

    /** An accent text link, such as "Back to tiles". */
    public static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeBoldFont());
        button.setForeground(Theme.accent());
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(JButton.LEFT);
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    /** A compact outlined button in the accent, for an action on one row of a list. */
    public static JButton rowButton(String text) {
        JButton button = new ActionButton(text, ActionButton.Kind.SECONDARY);
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setForeground(Theme.accent());
        button.setBorder(new EmptyBorder(4, 9, 4, 9));
        return button;
    }

    /** A primary button that stretches to the width of a vertical form. */
    public static JButton fullWidthButton(String text) {
        JButton button = primaryButton(text);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    /** A text field that shows its tooltip as faded placeholder text while it is empty. */
    public static JTextField textField(String tooltip) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(Theme.SURFACE_INSET);
                graphics.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.ARC, Theme.ARC);
                super.paintComponent(graphics);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Insets insets = getInsets();
                    graphics.setFont(getFont());
                    graphics.setColor(Theme.TEXT_MUTED);
                    graphics.drawString(tooltip, insets.left,
                            insets.top + graphics.getFontMetrics().getAscent());
                }
            }
        };
        field.setToolTipText(tooltip);
        field.setFont(FontManager.getRunescapeFont());
        field.setOpaque(false);
        field.setBackground(Theme.SURFACE_INSET);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setBorder(new CompoundBorder(new RoundedBorder(Theme.BORDER),
                new EmptyBorder(6, 9, 6, 9)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        return field;
    }

    public static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text, true);
        box.setFont(font(12f, Font.PLAIN));
        box.setForeground(Theme.TEXT_PRIMARY);
        box.setIcon(new CheckBoxIcon(false));
        box.setSelectedIcon(new CheckBoxIcon(true));
        box.setIconTextGap(8);
        box.setBorder(new EmptyBorder(0, 0, 0, 0));
        box.setOpaque(false);
        box.setFocusPainted(false);
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    private Components() {
    }
}
