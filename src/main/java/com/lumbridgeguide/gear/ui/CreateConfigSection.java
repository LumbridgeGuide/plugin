package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.util.LinkBrowser;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Color;

/** Creates a gear config on the website from what the player is wearing and carrying. */
class CreateConfigSection extends Section {

    private final GearConfigExportService gearConfigExportService;
    private final JCheckBox includeEquipmentBox;
    private final JCheckBox includeInventoryBox;
    private final JTextField configNameField;
    private final JButton createButton;
    private final WrapText statusLabel;
    private final JButton openButton;
    private String createdUrl;

    CreateConfigSection(GearConfigExportService gearConfigExportService) {
        super(0);
        this.gearConfigExportService = gearConfigExportService;

        includeEquipmentBox = GearForm.checkBox("Include equipment");
        includeInventoryBox = GearForm.checkBox("Include inventory");

        configNameField = GearForm.textField("Name (defaults to your name + setup)");
        configNameField.addActionListener(event -> onCreateClicked());

        createButton = GearForm.actionButton("Create");
        createButton.addActionListener(event -> onCreateClicked());

        statusLabel = GearForm.status(" ");

        openButton = Components.linkButton("Open on website  ›");
        openButton.setVisible(false);
        openButton.addActionListener(event -> {
            if (createdUrl != null) {
                LinkBrowser.browse(createdUrl);
            }
        });

        Card form = new Card(12);
        form.add(GearForm.title("Create config"));
        form.add(Box.createVerticalStrut(6));
        form.add(GearForm.help("Makes a gear config on the website from what you're wearing and carrying, rune "
                + "pouch included."));
        form.add(Box.createVerticalStrut(10));
        form.add(includeEquipmentBox);
        form.add(Box.createVerticalStrut(4));
        form.add(includeInventoryBox);
        form.add(Box.createVerticalStrut(10));
        form.add(Components.sectionLabel("Name"));
        form.add(Box.createVerticalStrut(5));
        form.add(configNameField);
        form.add(Box.createVerticalStrut(10));
        form.add(createButton);
        form.add(Box.createVerticalStrut(8));
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(openButton);
        add(form);
    }

    private void onCreateClicked() {
        if (!includeEquipmentBox.isSelected() && !includeInventoryBox.isSelected()) {
            showStatus("Include equipment, inventory or both.", Theme.ERROR);
            return;
        }
        createButton.setEnabled(false);
        createButton.setText("Creating...");
        openButton.setVisible(false);
        showStatus("Reading your items...", Theme.TEXT_MUTED);

        gearConfigExportService.create(configNameField.getText(), includeEquipmentBox.isSelected(),
                includeInventoryBox.isSelected(), result -> SwingUtilities.invokeLater(() ->
                {
                    createButton.setEnabled(true);
                    createButton.setText("Create");
                    showStatus(result.getMessage(), result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
                    createdUrl = result.getUrl();
                    openButton.setVisible(result.isSuccess() && createdUrl != null);
                    revalidate();
                }));
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
