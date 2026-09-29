package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

/** The building blocks both gear subtabs share, so their forms look and line up the same. */
final class GearForm {

    static final int TEXT_WIDTH = Components.CONTENT_WIDTH;

    private GearForm() {
    }

    static JLabel title(String text) {
        JLabel title = Components.label(text, 15f, Font.BOLD, Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }

    static JComponent help(String text) {
        JComponent help = Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        return help;
    }

    static JTextField textField(String tooltip) {
        return Components.textField(tooltip);
    }

    static JCheckBox checkBox(String text) {
        return Components.checkBox(text);
    }

    /** A muted line under a checkbox, lined up with the checkbox's text rather than its box. */
    static JComponent checkBoxHint(String text) {
        JPanel hint = new JPanel(new BorderLayout());
        hint.setOpaque(false);
        hint.setBorder(new EmptyBorder(0, 23, 0, 0));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.add(Components.wrapped(text, TEXT_WIDTH - 23, 10f, Font.PLAIN, Theme.TEXT_MUTED));
        hint.setMaximumSize(new Dimension(Integer.MAX_VALUE, hint.getPreferredSize().height));
        return hint;
    }

    static JButton actionButton(String text) {
        return Components.fullWidthButton(text);
    }

    static WrapText status(String text) {
        return Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);
    }
}
