package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTeamData;
import com.lumbridgeguide.data.PluginTileData;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Instant;
import java.time.Duration;

public class BoardHeaderPanel extends JPanel {

    private final JLabel titleLabel;
    private final JLabel teamLabel;
    private final JLabel statsLabel;
    private final JLabel timingLabel;

    public BoardHeaderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 4, 0));

        titleLabel = new JLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        titleLabel.setForeground(LumbridgeGuideTheme.BRAND_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);

        teamLabel = new JLabel();
        teamLabel.setFont(teamLabel.getFont().deriveFont(Font.BOLD, 12f));
        teamLabel.setAlignmentX(LEFT_ALIGNMENT);
        teamLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        statsLabel = new JLabel();
        statsLabel.setFont(statsLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statsLabel.setForeground(LumbridgeGuideTheme.TEXT_SECONDARY);
        statsLabel.setAlignmentX(LEFT_ALIGNMENT);
        statsLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        timingLabel = new JLabel();
        timingLabel.setFont(timingLabel.getFont().deriveFont(Font.PLAIN, 10f));
        timingLabel.setForeground(LumbridgeGuideTheme.TEXT_MUTED);
        timingLabel.setAlignmentX(LEFT_ALIGNMENT);
        timingLabel.setBorder(new EmptyBorder(2, 0, 0, 0));

        add(titleLabel);
        add(teamLabel);
        add(statsLabel);
        add(timingLabel);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(LumbridgeGuideTheme.SEPARATOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        JPanel separatorWrapper = new JPanel();
        separatorWrapper.setOpaque(false);
        separatorWrapper.setLayout(new BoxLayout(separatorWrapper, BoxLayout.Y_AXIS));
        separatorWrapper.setBorder(new EmptyBorder(6, 0, 0, 0));
        separatorWrapper.add(separator);
        separatorWrapper.setAlignmentX(LEFT_ALIGNMENT);
        add(separatorWrapper);
    }

    public void update(PluginBoardData board) {
        if (board == null) {
            titleLabel.setText("");
            teamLabel.setText("");
            statsLabel.setText("");
            timingLabel.setText("");
            return;
        }

        titleLabel.setText(board.getTitle());

        PluginTeamData team = board.getMyTeam();
        if (team != null && team.getName() != null) {
            Color teamColor = LumbridgeGuideTheme.parseTeamColor(team.getColor());
            teamLabel.setText("Team: " + team.getName());
            teamLabel.setForeground(teamColor);
            teamLabel.setVisible(true);
        } else {
            teamLabel.setVisible(false);
        }

        int totalTiles = board.getTiles() != null ? board.getTiles().size() : 0;
        long claimedTiles = board.getTiles() != null
                ? board.getTiles().stream().filter(PluginTileData::isClaimed).count()
                : 0;
        statsLabel.setText(board.getGridSize() + "×" + board.getGridSize()
                + " grid  •  " + claimedTiles + "/" + totalTiles + " claimed");

        timingLabel.setText(formatTiming(board.getStartsAt(), board.getEndsAt()));

        revalidate();
        repaint();
    }

    private static String formatTiming(String startsAt, String endsAt) {
        if (endsAt == null || endsAt.isEmpty()) {
            return "";
        }

        try {
            Instant end = Instant.parse(endsAt);
            Duration remaining = Duration.between(Instant.now(), end);

            if (remaining.isNegative()) {
                return "Ended";
            }

            long days = remaining.toDays();
            long hours = remaining.toHours() % 24;
            long minutes = remaining.toMinutes() % 60;

            if (days > 0) {
                return "Ends in " + days + "d " + hours + "h";
            } else if (hours > 0) {
                return "Ends in " + hours + "h " + minutes + "m";
            } else {
                return "Ends in " + minutes + "m";
            }
        } catch (Exception ignored) {
            return endsAt;
        }
    }
}

