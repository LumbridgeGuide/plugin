package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A row of tabs. {@link Style#UNDERLINE} spreads the tabs across the panel and underlines the open one, for the main
 * tabs; {@link Style#TEXT} is a lighter row of links for tabs inside a tab, such as the gear subtabs.
 */
public class TabBar extends JPanel {

    public enum Style {
        UNDERLINE,
        TEXT
    }

    private final List<String> names = new ArrayList<>();
    private final List<TabButton> buttons = new ArrayList<>();
    private final Consumer<String> onSelect;

    public TabBar(List<String> tabNames, Style style, Consumer<String> onSelect) {
        this.onSelect = onSelect;
        setOpaque(false);
        if (style == Style.UNDERLINE) {
            setLayout(new GridLayout(1, tabNames.size()));
        } else {
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        }

        for (String name : tabNames) {
            TabButton button = new TabButton(name, style);
            button.addActionListener(event -> select(name));
            if (style == Style.TEXT && !buttons.isEmpty()) {
                add(Box.createHorizontalStrut(12));
            }
            names.add(name);
            buttons.add(button);
            add(button);
        }

        select(tabNames.get(0));
    }

    public void select(String name) {
        for (int index = 0; index < names.size(); index++) {
            buttons.get(index).setSelected(names.get(index).equals(name));
        }
        onSelect.accept(name);
    }

    private static final class TabButton extends JButton {

        private final Style style;

        private TabButton(String text, Style style) {
            super(text);
            this.style = style;
            setFont(style == Style.UNDERLINE ? FontManager.getRunescapeFont() : FontManager.getRunescapeBoldFont());
            setBorder(style == Style.UNDERLINE ? new EmptyBorder(7, 0, 7, 0) : new EmptyBorder(2, 0, 2, 0));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (selected) {
                setForeground(Theme.ACCENT);
            } else {
                setForeground(style == Style.UNDERLINE ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
            }
            repaint();
        }

        /** Underlined tabs draw a hairline under every tab and a thicker accent line under the open one. */
        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (style != Style.UNDERLINE) {
                return;
            }
            int lineHeight = isSelected() ? 2 : 1;
            graphics.setColor(isSelected() ? Theme.ACCENT : Theme.BORDER_SUBTLE);
            graphics.fillRect(0, getHeight() - lineHeight, getWidth(), lineHeight);
        }
    }
}
