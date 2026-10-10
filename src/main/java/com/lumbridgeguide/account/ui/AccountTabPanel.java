package com.lumbridgeguide.account.ui;

import com.lumbridgeguide.account.AccountStatusText;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.ui.Badge;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.util.LinkBrowser;

/** The RuneScape account that is logged in, whether it is linked, and the button that syncs it. */
public class AccountTabPanel extends JPanel {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 24;

    private final InitialAvatar avatar;
    private final JLabel accountName;
    private final JLabel accountType;
    private final JPanel badgeHolder;
    private final WrapText statusText;
    private final JButton syncButton;
    private final JButton refreshButton;
    private final WrapText resultText;
    private final JLabel lastSyncedText;

    public AccountTabPanel(AccountSyncService accountSyncService) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        avatar = new InitialAvatar();
        accountName = Components.label(" ", 14f, Font.BOLD, Theme.TEXT_PRIMARY);
        accountType = Components.label(" ", 11f, Font.PLAIN, Theme.TEXT_MUTED);

        JPanel nameColumn = new JPanel();
        nameColumn.setLayout(new BoxLayout(nameColumn, BoxLayout.Y_AXIS));
        nameColumn.setOpaque(false);
        nameColumn.add(accountName);
        nameColumn.add(accountType);

        JPanel who = new JPanel(new BorderLayout(8, 0));
        who.setOpaque(false);
        who.setAlignmentX(LEFT_ALIGNMENT);
        who.add(avatar, BorderLayout.WEST);
        who.add(nameColumn, BorderLayout.CENTER);

        badgeHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        badgeHolder.setOpaque(false);
        badgeHolder.setAlignmentX(LEFT_ALIGNMENT);

        statusText = Components.wrapped(" ", TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);

        syncButton = Components.primaryButton("Sync now");
        syncButton.addActionListener(event -> accountSyncService.syncNow());

        refreshButton = Components.secondaryButton("Refresh");
        refreshButton.setToolTipText("Re-read your account and check its link status again");
        refreshButton.addActionListener(event -> accountSyncService.refresh());

        JPanel buttons = new JPanel(new BorderLayout(6, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        buttons.add(syncButton, BorderLayout.CENTER);
        buttons.add(refreshButton, BorderLayout.EAST);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));

        resultText = Components.wrapped(" ", TEXT_WIDTH, 12f, Font.PLAIN, Theme.TEXT_MUTED);
        lastSyncedText = Components.label(" ", 11f, Font.PLAIN, Theme.TEXT_MUTED);
        lastSyncedText.setAlignmentX(LEFT_ALIGNMENT);

        Card card = new Card(12);
        card.add(who);
        card.add(Box.createVerticalStrut(10));
        card.add(badgeHolder);
        card.add(Box.createVerticalStrut(6));
        card.add(statusText);
        card.add(Box.createVerticalStrut(10));
        card.add(buttons);
        card.add(Box.createVerticalStrut(8));
        card.add(resultText);
        card.add(lastSyncedText);

        JComponent disclosure = Components.wrapped(
                "Sync sends this account's name, account type, skill levels, XP and quest progress to Lumbridge "
                        + "Guide. The first Sync now links the account to you.",
                Components.CONTENT_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED);
        disclosure.setAlignmentX(LEFT_ALIGNMENT);

        JLabel heading = Components.sectionLabel("RuneScape account");
        heading.setBorder(new EmptyBorder(0, 0, 6, 0));

        add(heading);
        add(card);
        add(Box.createVerticalStrut(12));
        add(disclosure);

        JButton privacy = Components.linkButton("Privacy policy");
        privacy.addActionListener(event -> LinkBrowser.browse(LumbridgeGuideClient.websiteUrl() + "/privacy"));
        add(Box.createVerticalStrut(4));
        add(privacy);

        accountSyncService.setListener(view -> SwingUtilities.invokeLater(() -> show(view)));
    }

    private void show(AccountSyncService.AccountView view) {
        boolean loggedIn = view.getDisplayName() != null;
        accountName.setText(loggedIn ? view.getDisplayName() : "Not logged in");
        accountName.setForeground(loggedIn ? Theme.TEXT_PRIMARY : Theme.TEXT_MUTED);
        accountType.setText(loggedIn ? view.getAccountType() : " ");
        avatar.setInitial(loggedIn ? view.getDisplayName() : "?");

        Color tone = color(view.getStatusTone());
        badgeHolder.removeAll();
        if (view.getStatusBadge() != null && !view.getStatusBadge().isEmpty()) {
            badgeHolder.add(Badge.status(view.getStatusBadge(), tone));
        }
        badgeHolder.setVisible(badgeHolder.getComponentCount() > 0);
        statusText.setText(view.getStatusText());
        statusText.setForeground(tone == Theme.TEXT_MUTED ? Theme.TEXT_SECONDARY : tone);

        syncButton.setEnabled(loggedIn && !view.isBusy());
        syncButton.setText(view.isBusy() ? "Syncing..." : "Sync now");
        refreshButton.setEnabled(!view.isBusy());

        resultText.setText(view.getMessage() == null ? " " : view.getMessage());
        resultText.setForeground(color(view.getMessageTone()));
        lastSyncedText.setText(view.getLastSynced() == null ? " " : "Last synced at " + view.getLastSynced());
        revalidate();
        repaint();
    }

    private static Color color(AccountStatusText.Tone tone) {
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

    /** A round placeholder with the account's first letter, since the plugin has no account avatar. */
    private static final class InitialAvatar extends JComponent {

        private static final int SIZE = 30;

        private String initial = "?";

        void setInitial(String name) {
            initial = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            canvas.setColor(Theme.SURFACE_INSET);
            canvas.fillOval(0, 0, SIZE - 1, SIZE - 1);
            canvas.setColor(Theme.BORDER);
            canvas.drawOval(0, 0, SIZE - 1, SIZE - 1);
            canvas.setFont(Components.font(14f, Font.BOLD));
            canvas.setColor(Theme.TEXT_SECONDARY);
            int width = canvas.getFontMetrics().stringWidth(initial);
            canvas.drawString(initial, (SIZE - width) / 2, (SIZE + canvas.getFontMetrics().getAscent()) / 2 - 2);
            canvas.dispose();
        }
    }
}
