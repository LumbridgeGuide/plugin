package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import lombok.experimental.UtilityClass;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

@UtilityClass
public class GearForm {

    public static WrapText status(String text) {
        return Components.wrapped(text, Components.CONTENT_WIDTH, Components.REGULAR, Theme.TEXT_MUTED);
    }

    public static JComponent note(String text, int indent) {
        WrapText note = Components.wrapped(text, Components.CONTENT_WIDTH - indent, Components.SMALL, Theme.TEXT_MUTED);
        note.setBorder(new EmptyBorder(0, indent, 0, 0));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        return note;
    }

    public static JPanel fieldRow(JTextField field, JButton button) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.add(field, BorderLayout.CENTER);
        row.add(button, BorderLayout.EAST);
        return fullWidth(row);
    }

    public static JPanel sideBySide(JComponent left, JComponent right) {
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
