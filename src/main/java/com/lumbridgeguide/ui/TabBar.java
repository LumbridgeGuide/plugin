package com.lumbridgeguide.ui;

import net.runelite.client.ui.FontManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** A row of text tabs: the open one in the accent, the rest muted. */
public class TabBar extends JPanel {

    private final List<String> names = new ArrayList<>();
    private final List<JButton> buttons = new ArrayList<>();
    private final Consumer<String> onSelect;

    public TabBar(List<String> tabNames, Consumer<String> onSelect) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.onSelect = onSelect;
        setOpaque(false);

        for (String name : tabNames) {
            JButton button = new JButton(name);
            button.setFont(FontManager.getRunescapeBoldFont());
            button.setBorder(new EmptyBorder(2, 0, 2, 0));
            button.setContentAreaFilled(false);
            button.setFocusPainted(false);
            button.setOpaque(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            button.addActionListener(event -> select(name));
            if (!buttons.isEmpty()) {
                add(Box.createHorizontalStrut(12));
            }
            names.add(name);
            buttons.add(button);
            add(button);
        }

        select(tabNames.get(0));
    }

    void select(String name) {
        for (int index = 0; index < names.size(); index++) {
            buttons.get(index).setForeground(names.get(index).equals(name) ? Theme.ACCENT : Theme.TEXT_MUTED);
        }
        onSelect.accept(name);
    }
}
