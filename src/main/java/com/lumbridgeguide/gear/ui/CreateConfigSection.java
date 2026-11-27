package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import java.awt.Color;

public class CreateConfigSection extends Section {

    private final GearConfigExportService gearConfigExportService;
    private final JToggleButton includeEquipmentBox;
    private final JToggleButton includeInventoryBox;
    private final JTextField configNameField;
    private final JButton createButton;
    private final WrapText statusLabel;
    private final JButton openButton;
    private String createdUrl;

    public CreateConfigSection(GearConfigExportService gearConfigExportService) {
        super(0);
        this.gearConfigExportService = gearConfigExportService;

        includeEquipmentBox = Components.toggleChip("Equipment");
        includeInventoryBox = Components.toggleChip("Inventory");

        configNameField = Components.textField("Name (optional)");
        configNameField.addActionListener(event -> onCreateClicked());

        createButton = Components.primaryButton("Save");
        createButton.addActionListener(event -> onCreateClicked());

        statusLabel = GearForm.status(" ");

        openButton = Components.linkButton("Open on the website  ›");
        openButton.setVisible(false);
        openButton.addActionListener(event -> {
            if (createdUrl != null) {
                LinkBrowser.browse(createdUrl);
            }
        });

        add(Components.sectionHeader("Save your current setup"));
        add(Box.createVerticalStrut(8));
        add(GearForm.sideBySide(includeEquipmentBox, includeInventoryBox));
        add(Box.createVerticalStrut(10));
        add(GearForm.fieldRow(configNameField, createButton));
        add(Box.createVerticalStrut(4));
        add(GearForm.note("Leave blank to name it after you", 0));
        add(Box.createVerticalStrut(6));
        add(statusLabel);
        add(Box.createVerticalStrut(4));
        add(openButton);
    }

    private void onCreateClicked() {
        if (!createButton.isEnabled()) {
            return;
        }

        if (!includeEquipmentBox.isSelected() && !includeInventoryBox.isSelected()) {
            showStatus("Include equipment, inventory or both", Theme.ERROR);
            return;
        }

        createButton.setEnabled(false);
        createButton.setText("Saving...");
        openButton.setVisible(false);
        showStatus("Reading your items...", Theme.TEXT_MUTED);

        gearConfigExportService.create(configNameField.getText(), includeEquipmentBox.isSelected(),
                includeInventoryBox.isSelected(), result -> {
                    createButton.setEnabled(true);
                    createButton.setText("Save");
                    showStatus(result.getMessage(), result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
                    createdUrl = result.getUrl();
                    openButton.setVisible(result.isSuccess() && createdUrl != null);
                    revalidate();
                });
    }

    private void showStatus(String message, Color colour) {
        statusLabel.setText(message);
        statusLabel.setForeground(colour);
    }
}
