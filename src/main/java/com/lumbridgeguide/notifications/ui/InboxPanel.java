package com.lumbridgeguide.notifications.ui;

import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.notifications.InboxService;
import com.lumbridgeguide.notifications.data.NotificationActionData;
import com.lumbridgeguide.notifications.data.NotificationData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.client.util.LinkBrowser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
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
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/** The player's notifications. Bingo invites can be answered here; other notifications open on the website. */
public class InboxPanel extends Section {

    private static final int TEXT_WIDTH = Components.CONTENT_WIDTH - 44;

    private final InboxService inboxService;
    private final Section list;

    public InboxPanel(InboxService inboxService, Runnable onBack) {
        super(0);
        this.inboxService = inboxService;

        JButton back = Components.linkButton("‹  Back");
        back.setForeground(Theme.TEXT_SECONDARY);
        back.setFont(Components.font(12f, Font.PLAIN));
        back.addActionListener(event -> onBack.run());
        JButton markAll = Components.textLink("Mark all read");
        markAll.addActionListener(event -> inboxService.markAllRead(() -> SwingUtilities.invokeLater(this::load)));

        list = new Section(0);

        add(back);
        add(Box.createVerticalStrut(10));
        add(Components.sectionHeader("Notifications", markAll));
        add(Box.createVerticalStrut(2));
        add(list);
    }

    public void load() {
        list.removeAll();
        list.add(message("Loading...", Theme.TEXT_MUTED));
        refresh();
        inboxService.load(
                notifications -> SwingUtilities.invokeLater(() -> show(notifications)),
                failure -> SwingUtilities.invokeLater(() -> {
                    list.removeAll();
                    list.add(message(failure, Theme.ERROR));
                    refresh();
                }));
    }

    private void show(List<NotificationData> notifications) {
        list.removeAll();
        if (notifications.isEmpty()) {
            list.add(message("Nothing here yet. Bingo invites and proof results show up here.", Theme.TEXT_MUTED));
        }
        for (NotificationData notification : notifications) {
            list.add(row(notification));
        }
        refresh();
    }

    private static WrapText message(String text, Color colour) {
        WrapText message = Components.wrapped(text, Components.CONTENT_WIDTH, 11f, Font.PLAIN, colour);
        message.setBorder(new EmptyBorder(8, 0, 0, 0));
        return message;
    }

    private static boolean hasAnswer(NotificationData notification) {
        return notification.getActionTaken() == null && notification.getActions() != null
                && notification.getActions().stream().anyMatch(action -> "accept".equals(action.getAction()));
    }

    /**
     * One notification: a dot while it is unread, the title and when, the details, and for a bingo invite the buttons
     * that answer it. Others open on the website when clicked.
     */
    private JPanel row(NotificationData notification) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(9, 0, 9, 0)));
        boolean unread = !notification.isRead();
        JLabel dotLabel = new JLabel(new DotIcon(unread ? Theme.accent() : new Color(0, 0, 0, 0)));
        dotLabel.setVerticalAlignment(JLabel.TOP);
        dotLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        titleRow.add(Components.wrapped(notification.getTitle(), TEXT_WIDTH, 12f, unread ? Font.BOLD : Font.PLAIN,
                unread ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY), BorderLayout.CENTER);
        JLabel when = Components.label(TimeText.ago(notification.getCreatedAt()).replace(" ago", ""), 11f,
                Font.PLAIN, Theme.TEXT_MUTED);
        when.setVerticalAlignment(JLabel.TOP);
        titleRow.add(when, BorderLayout.EAST);
        text.add(titleRow);
        if (notification.getDescription() != null) {
            text.add(Box.createVerticalStrut(2));
            WrapText description = Components.wrapped(notification.getDescription(), TEXT_WIDTH + 24, 11f,
                    Font.PLAIN, Theme.TEXT_SECONDARY);
            description.setAlignmentX(LEFT_ALIGNMENT);
            text.add(description);
        }
        if (hasAnswer(notification)) {
            text.add(Box.createVerticalStrut(7));
            text.add(answerButtons(notification));
        }

        row.add(dotLabel, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        if (!hasAnswer(notification) && notification.getLink() != null && !notification.getLink().isEmpty()) {
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

    private JPanel answerButtons(NotificationData notification) {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttons.setOpaque(false);
        buttons.setBorder(new EmptyBorder(0, -6, 0, 0));
        buttons.setAlignmentX(LEFT_ALIGNMENT);
        for (NotificationActionData action : notification.getActions()) {
            if (!"accept".equals(action.getAction()) && !"decline".equals(action.getAction())) {
                continue;
            }
            JButton button = Components.smallButton(action.getLabel(), "accept".equals(action.getAction()));
            button.addActionListener(event -> {
                button.setEnabled(false);
                inboxService.act(notification.getId(), action.getAction(),
                        () -> SwingUtilities.invokeLater(this::load),
                        failure -> SwingUtilities.invokeLater(() -> {
                            button.setEnabled(true);
                            button.setToolTipText(failure);
                        }));
            });
            buttons.add(button);
        }
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, buttons.getPreferredSize().height));
        return buttons;
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}
