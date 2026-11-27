package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.List;
import java.util.Optional;

public class BankTabSection extends Section {

    private static final String BANK_CLOSED_HINT = "Open your bank to make the tab";

    private final GearTagService gearTagService;
    private final JCheckBox includeMissingBox;
    private final JTextField codeField;
    private final JButton generateButton;
    private final WrapText statusLabel;
    private final Section missingPanel;
    private boolean bankOpen;
    private boolean generating;
    private boolean showingBankHint = true;

    public BankTabSection(GearTagService gearTagService) {
        super(0);
        this.gearTagService = gearTagService;

        includeMissingBox = Components.checkBox("Include missing items");

        codeField = Components.textField("Setup code or link");
        codeField.addActionListener(event -> onGenerateClicked());

        generateButton = Components.primaryButton("Make tab");
        generateButton.setEnabled(false);
        generateButton.addActionListener(event -> onGenerateClicked());

        statusLabel = GearForm.status(BANK_CLOSED_HINT);

        missingPanel = new Section(0);
        missingPanel.setVisible(false);

        add(Components.sectionHeader("Bank tab"));
        add(Box.createVerticalStrut(8));
        add(GearForm.fieldRow(codeField, generateButton));
        add(Box.createVerticalStrut(6));
        add(statusLabel);
        add(Box.createVerticalStrut(10));
        add(includeMissingBox);
        add(Box.createVerticalStrut(2));
        add(GearForm.note("Shown as placeholders in the tab", 22));
        add(Box.createVerticalStrut(16));
        add(missingPanel);
    }

    public void setBankOpen(boolean open) {
        bankOpen = open;
        generateButton.setEnabled(open && !generating);
        if (!generating && !open) {
            showStatus(BANK_CLOSED_HINT, Theme.TEXT_MUTED);
            showingBankHint = true;
        } else if (!generating && showingBankHint) {
            showStatus(" ", Theme.TEXT_MUTED);
        }
    }

    private void onGenerateClicked() {
        if (!bankOpen || generating) {
            return;
        }
        Optional<String> code = GearTagService.extractCode(codeField.getText());
        if (code.isEmpty()) {
            showStatus("Enter a valid setup code", Theme.ERROR);
            return;
        }

        generating = true;
        generateButton.setEnabled(false);
        generateButton.setText("Making...");
        showStatus("Fetching the setup...", Theme.TEXT_MUTED);
        showMissing(List.of());

        gearTagService.generate(code.get(), includeMissingBox.isSelected(), result -> SwingUtilities.invokeLater(() -> {
            generating = false;
            generateButton.setEnabled(bankOpen);
            generateButton.setText("Make tab");
            showStatus(result.getMessage(), result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
            showMissing(result.getMissingItems());
        }));
    }

    private void showMissing(List<String> names) {
        missingPanel.removeAll();
        if (!names.isEmpty()) {
            missingPanel.add(Components.sectionHeader("Missing items", Components.label(String.valueOf(names.size()),
                    Components.SMALL, Theme.TEXT_MUTED)));
            missingPanel.add(Box.createVerticalStrut(6));
            for (String name : names) {
                missingPanel.add(Components.statusLine(name, Theme.WARNING, Theme.TEXT_PRIMARY));
                missingPanel.add(Box.createVerticalStrut(5));
            }
        }
        missingPanel.setVisible(!names.isEmpty());
        revalidate();
        repaint();
    }

    private void showStatus(String message, Color colour) {
        showingBankHint = false;
        statusLabel.setText(message);
        statusLabel.setForeground(colour);
    }
}
