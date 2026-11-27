package com.lumbridgeguide.ui;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;

public class EmptyState extends Section {

    public EmptyState(String title, String message, JButton... actions) {
        super(16);

        JLabel titleLabel = Components.label(title, Components.BOLD, Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        WrapText messageText = Components.wrapped(message, Components.CONTENT_WIDTH - 40, Components.REGULAR,
                Theme.TEXT_SECONDARY);
        messageText.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageText.setCentred(true);

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(messageText);
        if (actions.length > 0) {
            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actionRow.setOpaque(false);
            actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);
            for (JButton action : actions) {
                actionRow.add(action);
            }
            actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, actionRow.getPreferredSize().height));
            add(Box.createVerticalStrut(12));
            add(actionRow);
        }
    }
}
