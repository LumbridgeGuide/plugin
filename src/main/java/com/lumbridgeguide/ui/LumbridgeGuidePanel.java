package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.LumbridgeGuidePlugin;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public class LumbridgeGuidePanel extends PluginPanel {

    private final LumbridgeGuideConfig config;

    private final JLabel noKeyLabel;
    private final JButton syncButton;
    private final JLabel statusLabel;

    public LumbridgeGuidePanel(LumbridgeGuidePlugin plugin, LumbridgeGuideConfig config) {
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND);

        JLabel titleLabel = new JLabel("Lumbridge Guide");
        titleLabel.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND);

        noKeyLabel = new JLabel(
                "<html><center>Please set your API key in the<br>"
                        + "plugin settings to get started.</center></html>");
        noKeyLabel.setForeground(LumbridgeGuideTheme.TEXT_SECONDARY);
        noKeyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        noKeyLabel.setBorder(new EmptyBorder(20, 0, 20, 0));

        syncButton = new JButton("Sync Inventory");
        syncButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        syncButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        syncButton.setFocusPainted(false);
        syncButton.setBackground(LumbridgeGuideTheme.BRAND_ACCENT);
        syncButton.setForeground(LumbridgeGuideTheme.TEXT_PRIMARY);
        syncButton.addActionListener(event -> plugin.onSyncButtonClicked());

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(LumbridgeGuideTheme.TEXT_SECONDARY);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        contentPanel.add(noKeyLabel);
        contentPanel.add(syncButton);
        contentPanel.add(statusLabel);
        add(contentPanel, BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        SwingUtilities.invokeLater(() ->
        {
            boolean hasKey = config.apiKey() != null
                    && !config.apiKey().trim().isEmpty();

            noKeyLabel.setVisible(!hasKey);
            syncButton.setVisible(hasKey);
            statusLabel.setVisible(hasKey);

            revalidate();
            repaint();
        });
    }

    public void setStatus(String message, Color color) {
        SwingUtilities.invokeLater(() ->
        {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }

    public void setSyncing(boolean syncing) {
        SwingUtilities.invokeLater(() ->
        {
            syncButton.setEnabled(!syncing);
            syncButton.setText(syncing ? "Syncing..." : "Sync Inventory");
        });
    }
}
