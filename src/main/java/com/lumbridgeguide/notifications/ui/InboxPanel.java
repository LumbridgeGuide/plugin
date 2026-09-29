package com.lumbridgeguide.notifications.ui;

import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.notifications.InboxService;
import com.lumbridgeguide.notifications.data.NotificationActionData;
import com.lumbridgeguide.notifications.data.NotificationData;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;

/** The player's notifications. Bingo invites can be answered here; other notifications open on the website. */
public class InboxPanel extends Section {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 50;

    private final InboxService inboxService;
    private final JLabel countLabel;
    private final Section list;

    public InboxPanel(InboxService inboxService, Runnable onBack) {
        super(0);
        this.inboxService = inboxService;

        JButton back = Components.linkButton("‹  Back");
        back.addActionListener(event -> onBack.run());
        JButton markAll = Components.linkButton("Mark all read");
        markAll.setForeground(Theme.TEXT_SECONDARY);
        markAll.setFont(Components.font(10f, Font.PLAIN));
        markAll.addActionListener(event -> inboxService.markAllRead(() -> SwingUtilities.invokeLater(this::load)));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.add(back, BorderLayout.WEST);
        top.add(markAll, BorderLayout.EAST);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, top.getPreferredSize().height));

        countLabel = Components.sectionLabel("Notifications");
        list = new Section(0);

        add(top);
        add(Box.createVerticalStrut(10));
        add(countLabel);
        add(Box.createVerticalStrut(6));
        add(list);
    }

    public void load() {
        list.removeAll();
        list.add(Components.wrapped("Loading...", Components.CONTENT_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED));
        refresh();
        inboxService.load(
                notifications -> SwingUtilities.invokeLater(() -> show(notifications)),
                message -> SwingUtilities.invokeLater(() -> {
                    list.removeAll();
                    list.add(Components.wrapped(message, Components.CONTENT_WIDTH, 11f, Font.PLAIN, Theme.ERROR));
                    refresh();
                }));
    }

    private void show(List<NotificationData> notifications) {
        list.removeAll();
        long unread = notifications.stream().filter(notification -> !notification.isRead()).count();
        countLabel.setText(("Notifications  ·  " + unread + " new").toUpperCase(Locale.ROOT));
        if (notifications.isEmpty()) {
            list.add(Components.wrapped("Nothing here yet. Bingo invites and proof results show up here.",
                    Components.CONTENT_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED));
        }
        for (NotificationData notification : notifications) {
            list.add(hasAnswer(notification) ? inviteCard(notification) : row(notification));
            list.add(Box.createVerticalStrut(hasAnswer(notification) ? 10 : 0));
        }
        refresh();
    }

    private static boolean hasAnswer(NotificationData notification) {
        return notification.getActionTaken() == null && notification.getActions() != null
                && notification.getActions().stream().anyMatch(action -> "accept".equals(action.getAction()));
    }

    private Card inviteCard(NotificationData notification) {
        Card card = new Card(12);
        JLabel label = Components.sectionLabel("Bingo invite");
        label.setIcon(new DotIcon(Theme.accent()));
        label.setIconTextGap(6);
        card.add(label);
        card.add(Box.createVerticalStrut(6));
        card.add(Components.wrapped(notification.getTitle(), Components.CONTENT_WIDTH - 24, 12f, Font.PLAIN,
                Theme.TEXT_PRIMARY));
        if (notification.getDescription() != null) {
            card.add(Box.createVerticalStrut(3));
            card.add(Components.wrapped(notification.getDescription(), Components.CONTENT_WIDTH - 24, 10f,
                    Font.PLAIN, Theme.TEXT_MUTED));
        }
        card.add(Box.createVerticalStrut(10));
        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        for (NotificationActionData action : notification.getActions()) {
            if (!"accept".equals(action.getAction()) && !"decline".equals(action.getAction())) {
                continue;
            }
            JButton button = "accept".equals(action.getAction())
                    ? Components.primaryButton(action.getLabel())
                    : Components.secondaryButton(action.getLabel());
            button.addActionListener(event -> {
                button.setEnabled(false);
                inboxService.act(notification.getId(), action.getAction(),
                        () -> SwingUtilities.invokeLater(this::load),
                        message -> SwingUtilities.invokeLater(() -> {
                            button.setEnabled(true);
                            button.setToolTipText(message);
                        }));
            });
            buttons.add(button);
        }
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
        card.add(buttons);
        return card;
    }

    private JPanel row(NotificationData notification) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(8, 0, 8, 0)));
        Color dot = notification.isRead() ? Theme.TEXT_MUTED : Theme.accent();
        JLabel dotLabel = new JLabel(new DotIcon(dot));
        dotLabel.setVerticalAlignment(JLabel.TOP);
        dotLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
        String text = notification.getDescription() == null
                ? notification.getTitle() : notification.getTitle() + ". " + notification.getDescription();
        WrapText body = Components.wrapped(text, TEXT_WIDTH, 11f, Font.PLAIN,
                notification.isRead() ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY);
        JLabel when = Components.label(TimeText.ago(notification.getCreatedAt()).replace(" ago", ""), 10f,
                Font.PLAIN, Theme.TEXT_MUTED);
        when.setVerticalAlignment(JLabel.TOP);
        row.add(dotLabel, BorderLayout.WEST);
        row.add(body, BorderLayout.CENTER);
        row.add(when, BorderLayout.EAST);
        if (notification.getLink() != null && !notification.getLink().isEmpty()) {
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            row.setToolTipText("Open on the website");
            row.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    String link = notification.getLink();
                    LinkBrowser.browse(link.startsWith("http") ? link : LumbridgeGuideClient.websiteUrl() + link);
                }
            });
        }
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}
