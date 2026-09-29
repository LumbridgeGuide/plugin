package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginTeamData;
import com.lumbridgeguide.bingo.data.PluginTileData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.SwatchIcon;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Duration;
import java.time.Instant;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** The board's name, the viewer's team, time left, the team's progress and the code for screenshots. */
public class BoardHeaderPanel extends JPanel {

    private final WrapText titleLabel;
    private final JPanel teamRow;
    private final JLabel teamLabel;
    private final JLabel timingLabel;
    private final ProgressBar progressBar;
    private final JLabel statsLabel;
    private final JLabel codeCaption;
    private final VerificationCodePanel codePanel;

    public BoardHeaderPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 10, 0));

        titleLabel = Components.wrapped("", Components.CONTENT_WIDTH, 14f, Font.BOLD, Theme.TEXT_PRIMARY);

        teamLabel = Components.label("", 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
        teamLabel.setIconTextGap(6);
        timingLabel = Components.label("", 11f, Font.PLAIN, Theme.WARNING);
        teamRow = new JPanel(new BorderLayout(6, 0));
        teamRow.setOpaque(false);
        teamRow.setAlignmentX(LEFT_ALIGNMENT);
        teamRow.add(teamLabel, BorderLayout.CENTER);
        teamRow.add(timingLabel, BorderLayout.EAST);

        progressBar = new ProgressBar();
        statsLabel = Components.label("", 10f, Font.PLAIN, Theme.TEXT_MUTED);
        statsLabel.setAlignmentX(LEFT_ALIGNMENT);

        codeCaption = Components.sectionLabel("Verification code");
        codePanel = new VerificationCodePanel();
        codePanel.setAlignmentX(LEFT_ALIGNMENT);

        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(teamRow);
        add(Box.createVerticalStrut(6));
        add(progressBar);
        add(Box.createVerticalStrut(5));
        add(statsLabel);
        add(Box.createVerticalStrut(10));
        add(codeCaption);
        add(Box.createVerticalStrut(5));
        add(codePanel);
    }

    public void update(PluginBoardData board) {
        if (board == null) {
            titleLabel.setText("");
            teamLabel.setText("");
            timingLabel.setText("");
            statsLabel.setText("");
            codePanel.setCode(null);
            codeCaption.setVisible(false);
            return;
        }

        titleLabel.setText(board.getTitle());

        PluginTeamData team = board.getMyTeam();
        Color teamColor = team != null ? Theme.parseTeamColor(team.getColor()) : Theme.ACCENT;
        if (team != null && team.getName() != null) {
            teamLabel.setText("Team: " + team.getName());
            teamLabel.setIcon(new SwatchIcon(teamColor));
        } else {
            teamLabel.setText("");
            teamLabel.setIcon(null);
        }
        timingLabel.setText(formatTiming(board.getEndsAt()));
        teamRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, teamRow.getPreferredSize().height));

        int totalTiles = board.getTiles() != null ? board.getTiles().size() : 0;
        long claimedTiles = board.getTiles() != null
                ? board.getTiles().stream().filter(PluginTileData::isClaimed).count()
                : 0;
        progressBar.setProgress(totalTiles == 0 ? 0 : (double) claimedTiles / totalTiles, teamColor);
        statsLabel.setText(claimedTiles + " of " + totalTiles + " tiles claimed");

        boolean hasCode = board.getVerificationCode() != null && !board.getVerificationCode().isEmpty();
        codeCaption.setVisible(hasCode);
        codePanel.setCode(board.getVerificationCode());

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
