package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.SwatchIcon;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The top of the Bingo tab: the board's name with the board switcher, the viewer's team and the time left, then the
 * code players show in every proof screenshot.
 */
public class BoardHeaderPanel extends JPanel {

    private final WrapText titleLabel;
    private final JLabel metaLabel;
    private final JPanel codeHeader;
    private final VerificationCodePanel codePanel;

    BoardHeaderPanel(BoardNavigatorPanel navigator) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        titleLabel = Components.wrapped("", Components.CONTENT_WIDTH - 40, 14f, Font.BOLD, Theme.TEXT_PRIMARY);
        JPanel titleRow = new JPanel(new BorderLayout(6, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(LEFT_ALIGNMENT);
        titleRow.add(titleLabel, BorderLayout.CENTER);
        JPanel navigatorHolder = new JPanel(new BorderLayout());
        navigatorHolder.setOpaque(false);
        navigatorHolder.add(navigator, BorderLayout.NORTH);
        titleRow.add(navigatorHolder, BorderLayout.EAST);

        metaLabel = Components.label("", 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
        metaLabel.setIconTextGap(6);
        metaLabel.setAlignmentX(LEFT_ALIGNMENT);

        codePanel = new VerificationCodePanel();
        codePanel.setAlignmentX(LEFT_ALIGNMENT);
        codePanel.setToolTipText("Show this code in every proof screenshot. Click to copy.");
        JButton copy = Components.textLink("Copy");
        copy.addActionListener(event -> codePanel.copy());
        codeHeader = Components.sectionHeader("Board code", copy);

        add(titleRow);
        add(Box.createVerticalStrut(4));
        add(metaLabel);
        add(Box.createVerticalStrut(14));
        add(codeHeader);
        add(Box.createVerticalStrut(8));
        add(codePanel);
    }

    public void update(PluginBoardData board) {
        if (board == null) {
            titleLabel.setText("");
            metaLabel.setText("");
            codePanel.setCode(null);
            codeHeader.setVisible(false);
            return;
        }

        titleLabel.setText(board.getTitle());
        PluginTeamData team = board.getMyTeam();
        boolean hasTeam = team != null && team.getName() != null;
        String timing = formatTiming(board.getEndsAt());
        String teamName = hasTeam ? team.getName() : "No team yet";
        metaLabel.setText(timing.isEmpty() ? teamName : teamName + "  ·  " + timing);
        metaLabel.setIcon(hasTeam ? new SwatchIcon(Theme.parseTeamColor(team.getColor())) : null);

        boolean hasCode = board.getVerificationCode() != null && !board.getVerificationCode().isEmpty();
        codeHeader.setVisible(hasCode);
        codePanel.setCode(board.getVerificationCode());

        revalidate();
        repaint();
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static String formatTiming(String endsAt) {
        if (endsAt == null || endsAt.isEmpty()) {
            return "";
        }

        try {
            Duration remaining = Duration.between(Instant.now(), Instant.parse(endsAt));

            if (remaining.isNegative()) {
                return "Ended";
            }

            long days = remaining.toDays();
            long hours = remaining.toHours() % 24;
            long minutes = remaining.toMinutes() % 60;

            if (days > 0) {
                return "ends in " + days + "d " + hours + "h";
            } else if (hours > 0) {
                return "ends in " + hours + "h " + minutes + "m";
            }
            return "ends in " + minutes + "m";
        } catch (Exception ignored) {
            return "";
        }
    }
}
