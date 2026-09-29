package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.List;
import java.util.Optional;

/** Generates a bank tag tab from a gear code while the bank is open, then lists what was missing. */
class BankTabSection extends Section {

    private static final String BANK_CLOSED_HINT = "Open your bank to generate a tab.";

    private final GearTagService gearTagService;
    private final JCheckBox includeMissingBox;
    private final JTextField codeField;
    private final JButton generateButton;
    private final WrapText statusLabel;
    private final JPanel missingPanel;
    private boolean bankOpen;
    private boolean generating;
    private boolean showingBankHint = true;

    BankTabSection(GearTagService gearTagService) {
        super(0);
        this.gearTagService = gearTagService;

        includeMissingBox = GearForm.checkBox("Include missing items");

        codeField = GearForm.textField("Gear code or gear page link");
        codeField.addActionListener(event -> onGenerateClicked());

        generateButton = GearForm.actionButton("Generate");
        generateButton.setEnabled(false);
        generateButton.addActionListener(event -> onGenerateClicked());

        statusLabel = GearForm.status(BANK_CLOSED_HINT);

        missingPanel = new JPanel();
        missingPanel.setLayout(new BoxLayout(missingPanel, BoxLayout.Y_AXIS));
        missingPanel.setOpaque(false);
        missingPanel.setAlignmentX(LEFT_ALIGNMENT);
        missingPanel.setVisible(false);

        add(GearForm.title("Bank tab"));
        add(Box.createVerticalStrut(6));
        add(GearForm.help("Tag all items for a config and created a tag-tab with a defined layout."));
        add(Box.createVerticalStrut(10));
        add(Components.sectionLabel("Gear code or gear page link"));
        add(Box.createVerticalStrut(4));
        add(codeField);
        add(Box.createVerticalStrut(10));
        add(includeMissingBox);
        add(GearForm.checkBoxHint("Show items you don't have as placeholders in the tab"));
        add(Box.createVerticalStrut(10));
        add(generateButton);
        add(Box.createVerticalStrut(10));
        add(statusLabel);
        add(Box.createVerticalStrut(8));
        add(missingPanel);
    }

    /**
     * Generating reads the bank, so it is only offered while the bank is open. Called on the Swing thread when the
     * bank opens or closes.
     */
    void setBankOpen(boolean open) {
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
            showStatus("Enter a valid gear code", Theme.ERROR);
            return;
        }

        generating = true;
        generateButton.setEnabled(false);
        generateButton.setText("Generating...");
        showStatus("Fetching gear set...", Theme.TEXT_MUTED);
        showMissing(List.of());

        gearTagService.generate(code.get(), includeMissingBox.isSelected(), result -> SwingUtilities.invokeLater(() ->
        {
            generating = false;
            generateButton.setEnabled(bankOpen);
            generateButton.setText("Generate");
            showStatus(result.getMessage(), result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
            showMissing(result.getMissingItems());
        }));
    }

    private void showMissing(List<String> names) {
        missingPanel.removeAll();
        if (!names.isEmpty()) {
            missingPanel.add(Components.sectionLabel("Missing items  ·  " + names.size()));
            missingPanel.add(Box.createVerticalStrut(4));
            for (String name : names) {
                missingPanel.add(Components.statusLine(name, Theme.WARNING, Theme.TEXT_PRIMARY));
                missingPanel.add(Box.createVerticalStrut(3));
            }
        }
        missingPanel.setVisible(!names.isEmpty());
        revalidate();
        repaint();
    }

    private void showStatus(String message, Color color) {
        showingBankHint = false;
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
