package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.awt.Graphics;

public class TabButton extends JButton {

    public TabButton(String text) {
        super(text);
        setFont(FontManager.getRunescapeFont());
        setBorder(new EmptyBorder(7, 0, 7, 0));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        setForeground(selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        int lineHeight = isSelected() ? 2 : 1;
        graphics.setColor(isSelected() ? Theme.accent() : Theme.BORDER_SUBTLE);
        graphics.fillRect(0, getHeight() - lineHeight, getWidth(), lineHeight);
    }
}
