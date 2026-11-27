package com.lumbridgeguide.account.ui;

import com.lumbridgeguide.account.AccountAction;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.account.AccountView;
import com.lumbridgeguide.account.StatusTone;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class AccountFooter extends JPanel {

    private final AccountSyncService accountSyncService;
    private final JPanel accountPart;
    private final Section linkPanel;
    private final WrapText linkIntro;
    private final JButton linkSyncButton;
    private final JLabel statusLabel;
    private final JButton actionLink;
    private final WrapText detailText;
    private final JLabel syncedLabel;
    private AccountView view;
    private boolean linking;

    @Inject
    public AccountFooter(AccountSyncService accountSyncService) {
        this.accountSyncService = accountSyncService;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDER_SUBTLE), new EmptyBorder(8, 0, 0, 0)));

        linkIntro = Components.wrapped(" ", Components.CONTENT_WIDTH, Components.REGULAR, Theme.TEXT_SECONDARY);
        linkSyncButton = Components.fullWidthButton("Sync now");
        linkSyncButton.addActionListener(event -> accountSyncService.syncNow());
        linkPanel = linkPanel();

        statusLabel = Components.statusLine(" ", Theme.TEXT_MUTED, Theme.TEXT_SECONDARY);
        actionLink = Components.textLink(" ");
        actionLink.addActionListener(event -> onAction());
        detailText = Components.wrapped(" ", Components.CONTENT_WIDTH, Components.SMALL, Theme.TEXT_MUTED);

        accountPart = new JPanel();
        accountPart.setLayout(new BoxLayout(accountPart, BoxLayout.Y_AXIS));
        accountPart.setOpaque(false);
        accountPart.setAlignmentX(LEFT_ALIGNMENT);
        accountPart.add(linkPanel);
        accountPart.add(row(statusLabel, actionLink));
        accountPart.add(detailText);
        accountPart.add(Box.createVerticalStrut(4));

        JButton privacy = Components.textLink("Privacy policy");
        privacy.setForeground(Theme.TEXT_MUTED);
        privacy.addActionListener(event -> LinkBrowser.browse(LumbridgeGuideClient.websiteUrl() + "/privacy"));
        syncedLabel = Components.label(" ", Components.SMALL, Theme.TEXT_MUTED);

        add(accountPart);
        add(row(privacy, syncedLabel));

        accountSyncService.setListener(update -> SwingUtilities.invokeLater(() -> show(update)));
    }

    public void setAccountVisible(boolean visible) {
        accountPart.setVisible(visible);
    }

    private Section linkPanel() {
        JButton close = Components.textLink("Close");
        close.setForeground(Theme.TEXT_MUTED);
        close.addActionListener(event -> {
            linking = false;
            show(view);
        });

        Section panel = new Section(0);
        panel.setVisible(false);
        panel.add(Components.sectionHeader("Link this account", close));
        panel.add(Box.createVerticalStrut(8));
        panel.add(linkIntro);
        panel.add(Box.createVerticalStrut(6));

        for (String sent : new String[]{"Name and account type", "Skill levels and XP", "Quests"}) {
            panel.add(Components.statusLine(sent, Theme.TEXT_MUTED, Theme.TEXT_SECONDARY));
            panel.add(Box.createVerticalStrut(4));
        }

        panel.add(Box.createVerticalStrut(6));
        panel.add(linkSyncButton);
        panel.add(Box.createVerticalStrut(6));
        panel.add(Components.wrapped("Also sends the account hash, the number RuneLite uses to tell accounts apart",
                Components.CONTENT_WIDTH, Components.SMALL, Theme.TEXT_MUTED));
        panel.add(Box.createVerticalStrut(12));

        return panel;
    }

    private void onAction() {
        if (view == null) {
            return;
        }

        switch (view.getAction()) {
            case SYNC:
                accountSyncService.syncNow();
                break;
            case LINK:
                linking = true;
                show(view);
                break;
            case REFRESH:
                accountSyncService.refresh();
                break;
            default:
                break;
        }
    }

    private void show(AccountView update) {
        if (update == null) {
            return;
        }

        view = update;
        AccountAction action = update.getAction();
        linking = linking && action == AccountAction.LINK;

        linkPanel.setVisible(linking);
        linkIntro.setText("Sync now links " + update.getDisplayName() + " to your Lumbridge Guide account and sends:");
        linkSyncButton.setEnabled(!update.isBusy());
        linkSyncButton.setText(update.isBusy() ? "Syncing..." : "Sync now");

        Color tone = colour(update.getTone());
        boolean problem = update.getTone() == StatusTone.WARNING
                || update.getTone() == StatusTone.ERROR;
        statusLabel.setText(action == AccountAction.SYNC ? "Linked as " + update.getDisplayName() : update.getHeadline());
        statusLabel.setIcon(new DotIcon(tone));
        statusLabel.setForeground(problem ? tone : Theme.TEXT_SECONDARY);

        actionLink.setVisible(action != AccountAction.NONE && !linking);
        actionLink.setEnabled(!update.isBusy());
        actionLink.setText(update.isBusy() ? "Syncing..." : actionText(action));

        boolean showMessage = update.getMessage() != null && update.getMessageTone() != StatusTone.SUCCESS;
        String detail = showMessage ? update.getMessage() : update.getDetail();
        detailText.setText(detail.isEmpty() ? " " : detail);
        detailText.setForeground(showMessage ? colour(update.getMessageTone()) : Theme.TEXT_MUTED);
        detailText.setVisible(!detail.isEmpty());

        syncedLabel.setText(update.getLastSynced() == null ? " " : "Synced at " + update.getLastSynced());

        revalidate();
        repaint();
    }

    private static String actionText(AccountAction action) {
        switch (action) {
            case SYNC:
                return "Sync now";
            case LINK:
                return "Link this account";
            default:
                return "Refresh";
        }
    }

    private static JPanel row(JComponent left, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        return row;
    }

    private static Color colour(StatusTone tone) {
        switch (tone) {
            case SUCCESS:
                return Theme.SUCCESS;
            case WARNING:
                return Theme.WARNING;
            case ERROR:
                return Theme.ERROR;
            default:
                return Theme.TEXT_MUTED;
        }
    }
}
