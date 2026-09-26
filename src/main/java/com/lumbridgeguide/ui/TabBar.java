package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A row of equal width tabs with an accent underline on the selected one.
 */
class TabBar extends JPanel {

    private final List<String> names = new ArrayList<>();
    private final List<JButton> buttons = new ArrayList<>();
    private final Consumer<String> onSelect;

    TabBar(List<String> tabNames, Consumer<String> onSelect) {
        super(new GridLayout(1, tabNames.size()));
        this.onSelect = onSelect;
        setOpaque(false);

        for (String name : tabNames) {
            JButton button = new JButton(name);
            button.setFont(FontManager.getRunescapeFont());
            button.setFocusPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(event -> select(name));
            names.add(name);
            buttons.add(button);
            add(button);
        }

        select(tabNames.get(0));
    }

    void select(String name) {
        for (int index = 0; index < names.size(); index++) {
            boolean selected = names.get(index).equals(name);
            JButton button = buttons.get(index);
            Color underline = selected ? Theme.ACCENT : Theme.BORDER;
            button.setForeground(selected ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, underline),
                    BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        }
        onSelect.accept(name);
    }
}
