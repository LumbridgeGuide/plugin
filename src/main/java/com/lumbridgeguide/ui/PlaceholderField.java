package com.lumbridgeguide.ui;

import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Insets;

public class PlaceholderField extends JTextField {

    private final String placeholder;

    public PlaceholderField(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(Theme.SURFACE_OVERLAY);
        graphics.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.ARC, Theme.ARC);
        super.paintComponent(graphics);
        if (getText().isEmpty() && !isFocusOwner()) {
            Insets insets = getInsets();
            graphics.setFont(getFont());
            graphics.setColor(Theme.TEXT_MUTED);
            graphics.drawString(placeholder, insets.left, insets.top + graphics.getFontMetrics().getAscent());
        }
    }
}
