package com.lumbridgeguide.account.ui;

import com.lumbridgeguide.account.AccountStatusText;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.ui.LumbridgeGuideTheme;
import com.lumbridgeguide.ui.Ui;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** The logged-in account's link status and the Sync now button. */
public class AccountTabPanel extends JPanel {

    private final JLabel accountName;
    private final Ui.WrapText statusText;
    private final JButton syncButton;
    private final JButton refreshButton;
    private final Ui.WrapText resultText;
    private final JLabel lastSyncedText;

    public AccountTabPanel(AccountSyncService accountSyncService) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        JLabel heading = Ui.label("RuneScape account", 15f, Font.BOLD, LumbridgeGuideTheme.TEXT_PRIMARY);
        heading.setAlignmentX(LEFT_ALIGNMENT);

        accountName = Ui.label(" ", 14f, Font.BOLD, LumbridgeGuideTheme.ACCENT);
        accountName.setAlignmentX(LEFT_ALIGNMENT);

        statusText = Ui.wrapped(" ", Ui.CONTENT_WIDTH, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_SECONDARY);

        syncButton = Ui.primaryButton("Sync now");
        syncButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        syncButton.setAlignmentX(LEFT_ALIGNMENT);
        syncButton.addActionListener(event -> accountSyncService.syncNow());

        refreshButton = Ui.secondaryButton("Refresh");
        refreshButton.setToolTipText("Re-read your account and check its link status again");
        refreshButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        refreshButton.setAlignmentX(LEFT_ALIGNMENT);
        refreshButton.addActionListener(event -> accountSyncService.refresh());

        resultText = Ui.wrapped(" ", Ui.CONTENT_WIDTH, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        lastSyncedText = Ui.label(" ", 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        lastSyncedText.setAlignmentX(LEFT_ALIGNMENT);

        JComponent disclosure = Ui.wrapped(
                "Sync sends this account's name, account type, skill levels, XP and quest progress to Lumbridge "
                        + "Guide. The first Sync now links the account to you.",
                Ui.CONTENT_WIDTH, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        disclosure.setAlignmentX(LEFT_ALIGNMENT);

        add(heading);
        add(Box.createVerticalStrut(6));
        add(accountName);
        add(Box.createVerticalStrut(4));
        add(statusText);
        add(Box.createVerticalStrut(10));
        add(syncButton);
        add(Box.createVerticalStrut(6));
        add(refreshButton);
        add(Box.createVerticalStrut(8));
        add(resultText);
        add(Box.createVerticalStrut(2));
        add(lastSyncedText);
        add(Box.createVerticalStrut(14));
        add(disclosure);

        accountSyncService.setListener(view -> SwingUtilities.invokeLater(() -> show(view)));
    }

    private void show(AccountSyncService.AccountView view) {
        boolean loggedIn = view.getDisplayName() != null;
        accountName.setText(loggedIn ? view.getDisplayName() : "Not logged in");
        accountName.setForeground(loggedIn ? LumbridgeGuideTheme.ACCENT : LumbridgeGuideTheme.TEXT_MUTED);
        statusText.setText(view.getStatusText());
        statusText.setForeground(color(view.getStatusTone()));

        syncButton.setEnabled(loggedIn && !view.isBusy());
        syncButton.setText(view.isBusy() ? "Syncing..." : "Sync now");
        refreshButton.setEnabled(!view.isBusy());

        resultText.setText(view.getMessage() == null ? " " : view.getMessage());
        resultText.setForeground(color(view.getMessageTone()));
        lastSyncedText.setText(view.getLastSynced() == null ? " " : "Last synced at " + view.getLastSynced());
    }

    private static Color color(AccountStatusText.Tone tone) {
        switch (tone) {
            case SUCCESS:
                return LumbridgeGuideTheme.SUCCESS;
            case WARNING:
                return LumbridgeGuideTheme.WARNING;
            case ERROR:
                return LumbridgeGuideTheme.ERROR;
            default:
                return LumbridgeGuideTheme.TEXT_MUTED;
        }
    }
}
