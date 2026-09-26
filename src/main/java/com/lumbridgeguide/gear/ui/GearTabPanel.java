package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Optional;

public class GearTabPanel extends JPanel {

    private final GearTagService gearTagService;
    private final GearConfigExportService gearConfigExportService;

    private final JTextField codeField;
    private final JButton syncButton;
    private final WrapText statusLabel;

    private final JTextField configNameField;
    private final JCheckBox includeEquipmentBox;
    private final JCheckBox includeInventoryBox;
    private final JButton createButton;
    private final WrapText createStatusLabel;
    private final JButton openButton;
    private String createdUrl;

    public GearTabPanel(GearTagService gearTagService, GearConfigExportService gearConfigExportService) {
        this.gearTagService = gearTagService;
        this.gearConfigExportService = gearConfigExportService;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel heading = Components.label("Gear bank tag", 15f, Font.BOLD, Theme.TEXT_PRIMARY);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        JComponent help = Components.wrapped(
                "Paste a gear code from its page on the website. Sync makes a bank tag with every item in the set.",
                Components.CONTENT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        help.setAlignmentX(LEFT_ALIGNMENT);

        codeField = textField("Gear code");
        codeField.addActionListener(event -> onSyncClicked());

        syncButton = Components.primaryButton("Sync");
        syncButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        syncButton.setAlignmentX(LEFT_ALIGNMENT);
        syncButton.addActionListener(event -> onSyncClicked());

        statusLabel = Components.wrapped(" ", Components.CONTENT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);

        add(heading);
        add(Box.createVerticalStrut(6));
        add(help);
        add(Box.createVerticalStrut(10));
        add(codeField);
        add(Box.createVerticalStrut(8));
        add(syncButton);
        add(Box.createVerticalStrut(10));
        add(statusLabel);

        JLabel createHeading = Components.label("Create gear config", 15f, Font.BOLD, Theme.TEXT_PRIMARY);
        createHeading.setAlignmentX(LEFT_ALIGNMENT);
        JComponent createHelp = Components.wrapped(
                "Make a gear config on the website from what you're wearing and carrying, rune pouch included.",
                Components.CONTENT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        createHelp.setAlignmentX(LEFT_ALIGNMENT);

        configNameField = textField("Name (defaults to your name + setup)");
        includeEquipmentBox = checkBox("Include equipment");
        includeInventoryBox = checkBox("Include inventory");

        createButton = Components.primaryButton("Create");
        createButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        createButton.setAlignmentX(LEFT_ALIGNMENT);
        createButton.addActionListener(event -> onCreateClicked());

        createStatusLabel = Components.wrapped(" ", Components.CONTENT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);

        openButton = Components.secondaryButton("Open on website");
        openButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        openButton.setAlignmentX(LEFT_ALIGNMENT);
        openButton.setVisible(false);
        openButton.addActionListener(event -> {
            if (createdUrl != null) {
                LinkBrowser.browse(createdUrl);
            }
        });

        add(Box.createVerticalStrut(18));
        add(createHeading);
        add(Box.createVerticalStrut(6));
        add(createHelp);
        add(Box.createVerticalStrut(10));
        add(configNameField);
        add(Box.createVerticalStrut(6));
        add(includeEquipmentBox);
        add(includeInventoryBox);
        add(Box.createVerticalStrut(8));
        add(createButton);
        add(Box.createVerticalStrut(8));
        add(createStatusLabel);
        add(Box.createVerticalStrut(6));
        add(openButton);
    }

    private void onCreateClicked() {
        if (!includeEquipmentBox.isSelected() && !includeInventoryBox.isSelected()) {
            showCreateStatus("Include equipment, inventory or both.", Theme.ERROR);
            return;
        }
        createButton.setEnabled(false);
        createButton.setText("Creating...");
        openButton.setVisible(false);
        showCreateStatus("Reading your items...", Theme.TEXT_MUTED);

        gearConfigExportService.create(configNameField.getText(), includeEquipmentBox.isSelected(),
                includeInventoryBox.isSelected(), result -> SwingUtilities.invokeLater(() ->
                {
                    createButton.setEnabled(true);
                    createButton.setText("Create");
                    showCreateStatus(result.getMessage(),
                            result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
                    createdUrl = result.getUrl();
                    openButton.setVisible(result.isSuccess() && createdUrl != null);
                    revalidate();
                }));
    }

    private void showCreateStatus(String message, Color color) {
        createStatusLabel.setText(message);
        createStatusLabel.setForeground(color);
    }

    private static JTextField textField(String tooltip) {
        JTextField field = new JTextField();
        field.setToolTipText(tooltip);
        field.setFont(FontManager.getRunescapeFont());
        field.setBackground(Theme.SURFACE_INSET);
        field.setForeground(Theme.TEXT_PRIMARY);
        field.setCaretColor(Theme.TEXT_PRIMARY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        field.setAlignmentX(LEFT_ALIGNMENT);
        return field;
    }

    private static JCheckBox checkBox(String text) {
        JCheckBox box = new JCheckBox(text, true);
        box.setFont(FontManager.getRunescapeFont());
        box.setForeground(Theme.TEXT_SECONDARY);
        box.setOpaque(false);
        box.setFocusPainted(false);
        box.setAlignmentX(LEFT_ALIGNMENT);
        return box;
    }

    private void onSyncClicked() {
        Optional<String> code = GearTagService.extractCode(codeField.getText());
        if (code.isEmpty()) {
            showStatus("Enter a valid gear code", Theme.ERROR);
            return;
        }

        syncButton.setEnabled(false);
        syncButton.setText("Syncing...");
        showStatus("Fetching gear set...", Theme.TEXT_MUTED);

        gearTagService.sync(code.get(), result -> SwingUtilities.invokeLater(() ->
        {
            syncButton.setEnabled(true);
            syncButton.setText("Sync");
            showStatus(result.getMessage(),
                    result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
        }));
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
