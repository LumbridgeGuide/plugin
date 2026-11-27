package com.lumbridgeguide.ui;

import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TabBar extends JPanel {

    private final List<String> names = new ArrayList<>();
    private final List<TabButton> buttons = new ArrayList<>();
    private final Consumer<String> onSelect;

    public TabBar(List<String> tabNames, Consumer<String> onSelect) {
        this.onSelect = onSelect;
        setOpaque(false);
        setLayout(new GridLayout(1, tabNames.size()));

        for (String name : tabNames) {
            TabButton button = new TabButton(name);
            button.addActionListener(event -> select(name));
            names.add(name);
            buttons.add(button);
            add(button);
        }

        select(tabNames.get(0));
    }

    private void select(String name) {
        for (int index = 0; index < names.size(); index++) {
            buttons.get(index).setSelected(names.get(index).equals(name));
        }

        onSelect.accept(name);
    }
}
