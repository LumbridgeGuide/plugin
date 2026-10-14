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

    static JComponent help(String text) {
        JComponent help = Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        return help;
    }

    /** A small label above a field, such as "Name". */
    static JLabel fieldLabel(String text) {
        JLabel label = Components.label(text, 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static JTextField textField(String tooltip) {
        return Components.textField(tooltip);
    }

    static JCheckBox checkBox(String text) {
        return Components.checkBox(text);
    }

    static JButton actionButton(String text) {
        return Components.fullWidthButton(text);
    }

    static WrapText status(String text) {
        return Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);
    }
}
