package com.lumbridgeguide.ui;

import com.lumbridgeguide.service.GearTagService;
import net.runelite.client.ui.FontManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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

    private final JTextField codeField;
    private final JButton syncButton;
    private final Ui.WrapText statusLabel;

    public GearTabPanel(GearTagService gearTagService) {
        this.gearTagService = gearTagService;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel heading = Ui.label("Gear bank tag", 15f, Font.BOLD, LumbridgeGuideTheme.TEXT_PRIMARY);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        JComponent help = Ui.wrapped(
                "Paste a gear code from its page on the website. Sync makes a bank tag with every item in the set.",
                Ui.CONTENT_WIDTH, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_SECONDARY);
        help.setAlignmentX(LEFT_ALIGNMENT);

        codeField = new JTextField();
        codeField.setToolTipText("Gear code");
        codeField.setFont(FontManager.getRunescapeFont());
        codeField.setBackground(LumbridgeGuideTheme.SURFACE_INSET);
        codeField.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        codeField.setCaretColor(LumbridgeGuideTheme.TEXT_PRIMARY);
        codeField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LumbridgeGuideTheme.BORDER),
                new EmptyBorder(6, 8, 6, 8)));
        codeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        codeField.setAlignmentX(LEFT_ALIGNMENT);
        codeField.addActionListener(event -> onSyncClicked());

        syncButton = Ui.primaryButton("Sync");
        syncButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        syncButton.setAlignmentX(LEFT_ALIGNMENT);
        syncButton.addActionListener(event -> onSyncClicked());

        statusLabel = Ui.wrapped(" ", Ui.CONTENT_WIDTH, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);

        add(heading);
        add(Box.createVerticalStrut(6));
        add(help);
        add(Box.createVerticalStrut(10));
        add(codeField);
        add(Box.createVerticalStrut(8));
        add(syncButton);
        add(Box.createVerticalStrut(10));
        add(statusLabel);
    }

    private void onSyncClicked() {
        Optional<String> code = GearTagService.extractCode(codeField.getText());
        if (!code.isPresent()) {
            showStatus("Enter a valid gear code", LumbridgeGuideTheme.ERROR);
            return;
        }

        syncButton.setEnabled(false);
        syncButton.setText("Syncing...");
        showStatus("Fetching gear set...", LumbridgeGuideTheme.TEXT_MUTED);

        gearTagService.sync(code.get(), result -> SwingUtilities.invokeLater(() ->
        {
            syncButton.setEnabled(true);
            syncButton.setText("Sync");
            showStatus(result.getMessage(),
                    result.isSuccess() ? LumbridgeGuideTheme.SUCCESS : LumbridgeGuideTheme.ERROR);
        }));
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
}
