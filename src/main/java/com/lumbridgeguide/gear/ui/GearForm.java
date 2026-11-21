package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import lombok.experimental.UtilityClass;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.Font;

@UtilityClass
class GearForm {

    static final int TEXT_WIDTH = Components.CONTENT_WIDTH;

    static JComponent help(String text) {
        JComponent help = Components.wrapped(text, TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        help.setAlignmentX(Component.LEFT_ALIGNMENT);
        return help;
    }

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
