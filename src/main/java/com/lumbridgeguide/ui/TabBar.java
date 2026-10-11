package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A row of tabs. {@link Style#UNDERLINE} spreads the tabs across the panel and underlines the open one in the accent,
 * like the website's header, for the main tabs; {@link Style#PILL} is a row of pills, the open one on a wash of the
 * accent, for views inside a tab such as the gear pages.
 */
public class TabBar extends JPanel {

    public enum Style {
        UNDERLINE,
        PILL
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
            if (style == Style.PILL && !buttons.isEmpty()) {
                add(Box.createHorizontalStrut(2));
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
            setFont(FontManager.getRunescapeFont());
            setBorder(style == Style.UNDERLINE ? new EmptyBorder(7, 0, 7, 0) : new EmptyBorder(3, 7, 3, 7));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public void setSelected(boolean selected) {
            super.setSelected(selected);
            if (style == Style.UNDERLINE) {
                setForeground(selected ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
            } else {
                setForeground(selected ? Theme.accentText() : Theme.TEXT_SECONDARY);
            }
            repaint();
        }

        /**
         * Underlined tabs draw a hairline under every tab and a thicker accent line under the open one. The open pill
         * sits on a wash of the accent.
         */
        @Override
        protected void paintComponent(Graphics graphics) {
            if (style == Style.PILL && isSelected()) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                canvas.setColor(Theme.accentWash());
                canvas.fillRoundRect(0, 0, getWidth(), getHeight(), Theme.ARC, Theme.ARC);
                canvas.dispose();
            }
            super.paintComponent(graphics);
            if (style != Style.UNDERLINE) {
                return;
            }
            int lineHeight = isSelected() ? 2 : 1;
            graphics.setColor(isSelected() ? Theme.accent() : Theme.BORDER_SUBTLE);
            graphics.fillRect(0, getHeight() - lineHeight, getWidth(), lineHeight);
        }
    }
}
