package com.lumbridgeguide.ui;

import com.lumbridgeguide.data.PluginBoardData;
import com.lumbridgeguide.data.PluginTeamData;
import com.lumbridgeguide.data.PluginTileData;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Font;
import java.time.Duration;
import java.time.Instant;

public class BoardHeaderPanel extends JPanel {

    private final JLabel codeCaption;
    private final VerificationCodePanel codePanel;
    private final Ui.WrapText titleLabel;
    private final JLabel teamLabel;
    private final JLabel statsLabel;
    private final JLabel timingLabel;

    public BoardHeaderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 8, 0));

        codeCaption = Ui.label("Verification code", 11f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        codeCaption.setAlignmentX(LEFT_ALIGNMENT);
        codeCaption.setBorder(new EmptyBorder(0, 0, 4, 0));

        codePanel = new VerificationCodePanel();
        codePanel.setAlignmentX(LEFT_ALIGNMENT);

        titleLabel = Ui.wrapped("", Ui.CONTENT_WIDTH, 15f, Font.BOLD, LumbridgeGuideTheme.TEXT_PRIMARY);

        teamLabel = Ui.label("", 12f, Font.BOLD, LumbridgeGuideTheme.TEXT_SECONDARY);
        teamLabel.setAlignmentX(LEFT_ALIGNMENT);
        teamLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        statsLabel = Ui.label("", 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_SECONDARY);
        statsLabel.setAlignmentX(LEFT_ALIGNMENT);
        statsLabel.setBorder(new EmptyBorder(4, 0, 0, 0));

        timingLabel = Ui.label("", 11f, Font.PLAIN, LumbridgeGuideTheme.TEXT_MUTED);
        timingLabel.setAlignmentX(LEFT_ALIGNMENT);
        timingLabel.setBorder(new EmptyBorder(2, 0, 0, 0));

        add(codeCaption);
        add(codePanel);
        add(Box.createVerticalStrut(10));
        add(titleLabel);
        add(teamLabel);
        add(statsLabel);
        add(timingLabel);
    }

    public void update(PluginBoardData board) {
        if (board == null) {
            titleLabel.setText("");
            teamLabel.setText("");
            statsLabel.setText("");
            timingLabel.setText("");
            codePanel.setCode(null);
            codeCaption.setVisible(false);
            return;
        }

        boolean hasCode = board.getVerificationCode() != null && !board.getVerificationCode().isEmpty();
        codeCaption.setVisible(hasCode);
        codePanel.setCode(board.getVerificationCode());

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
        statsLabel.setText(claimedTiles + " of " + totalTiles + " tiles claimed");

        timingLabel.setText(formatTiming(board.getEndsAt()));

        revalidate();
        repaint();
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
                return "Ends in " + days + "d " + hours + "h";
            } else if (hours > 0) {
                return "Ends in " + hours + "h " + minutes + "m";
            }
            return "Ends in " + minutes + "m";
        } catch (Exception ignored) {
            return endsAt;
        }
    }
}
