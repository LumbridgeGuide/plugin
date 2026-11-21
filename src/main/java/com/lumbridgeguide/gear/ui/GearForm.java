package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import lombok.experimental.UtilityClass;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

@UtilityClass
class GearForm {

    static final int TEXT_WIDTH = Components.CONTENT_WIDTH;

    static JTextField textField(String placeholder) {
        return Components.textField(placeholder);
    }

    static JCheckBox checkBox(String text) {
        return Components.checkBox(text);
    }

    static JButton actionButton(String text) {
        return Components.primaryButton(text);
    }

    static WrapText status(String text) {
        return Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);
    }

    static JComponent note(String text, int indent) {
        WrapText note = Components.wrapped(text, TEXT_WIDTH - indent, 11f, Font.PLAIN, Theme.TEXT_MUTED);
        note.setBorder(new EmptyBorder(0, indent, 0, 0));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        return note;
    }

    static JPanel fieldRow(JTextField field, JButton button) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return fullWidth(row);
    }

    static JPanel sideBySide(JComponent left, JComponent right) {
        JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
        row.setOpaque(false);
        row.add(left);
        row.add(right);
        return fullWidth(row);
    }

    private static JPanel fullWidth(JPanel row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
