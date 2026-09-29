package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginClaimData;
import com.lumbridgeguide.bingo.data.PluginStandingData;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.SwatchIcon;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * The board's standings or its recent claims, depending on {@link Mode}. Hidden boards send neither while their claims
 * are secret, so this says so instead.
 */
class StandingsPanel extends Section {

    enum Mode {
        STANDINGS,
        ACTIVITY
    }

    private static final String SECRET_MESSAGE = "This board hides other teams' claims until the winners are revealed.";

    private final Mode mode;

    StandingsPanel(Mode mode) {
        super(0);
        this.mode = mode;
    }

    void update(PluginBoardData board) {
        removeAll();
        if (board != null) {
            if (mode == Mode.STANDINGS) {
                showStandings(board);
            } else {
                showActivity(board);
            }
        }
        revalidate();
        repaint();
    }

    private void showStandings(PluginBoardData board) {
        Card card = new Card(12);
        card.add(Components.sectionLabel("Standings  ·  " + (board.getTiles() == null ? 0 : board.getTiles().size())
                + " tiles"));
        card.add(Box.createVerticalStrut(4));
        List<PluginStandingData> standings = board.getStandings();
        if (standings == null) {
            card.add(message(SECRET_MESSAGE));
        } else {
            int top = standings.stream().mapToInt(PluginStandingData::getScore).max().orElse(0);
            String myTeamId = board.getMyTeam() == null ? null : board.getMyTeam().getId();
            for (int index = 0; index < standings.size(); index++) {
                card.add(standingRow(standings.get(index), top, board.isTilePointsEnabled(),
                        standings.get(index).getTeamId().equals(myTeamId), index == standings.size() - 1));
            }
        }
        add(card);
    }

    private JPanel standingRow(PluginStandingData standing, int top, boolean points, boolean mine, boolean last) {
        Color colour = Theme.parseTeamColor(standing.getColor());
        JPanel row = new JPanel(new BorderLayout(0, 5));
        row.setOpaque(false);
        Border rule = BorderFactory.createMatteBorder(0, 0, last ? 0 : 1, 0, Theme.BORDER_SUBTLE);
        Border bar = mine ? BorderFactory.createMatteBorder(0, 2, 0, 0, colour) : new EmptyBorder(0, 0, 0, 0);
        row.setBorder(BorderFactory.createCompoundBorder(rule,
                BorderFactory.createCompoundBorder(bar, new EmptyBorder(7, mine ? 6 : 0, 7, 0))));

        JLabel name = Components.label(standing.getName(), 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
        name.setIcon(new SwatchIcon(colour));
        name.setIconTextGap(6);
        JLabel rank = Components.monoLabel(String.valueOf(standing.getRank()), 11f, Font.BOLD, Theme.TEXT_MUTED);
        String unit = points ? " pts" : standing.getScore() == 1 ? " tile" : " tiles";
        JLabel score = Components.monoLabel(standing.getScore() + unit, 11f, Font.BOLD,
                mine ? colour : Theme.TEXT_SECONDARY);

        JPanel line = new JPanel(new BorderLayout(6, 0));
        line.setOpaque(false);
        line.add(rank, BorderLayout.WEST);
        line.add(name, BorderLayout.CENTER);
        line.add(score, BorderLayout.EAST);

        ProgressBar progress = new ProgressBar();
        progress.setProgress(top == 0 ? 0 : (double) standing.getScore() / top, colour);

        row.add(line, BorderLayout.NORTH);
        row.add(progress, BorderLayout.SOUTH);
        return fitWidth(row);
    }

    private void showActivity(PluginBoardData board) {
        JLabel heading = Components.sectionLabel("Recent claims");
        heading.setBorder(new EmptyBorder(0, 0, 4, 0));
        add(heading);
        List<PluginClaimData> claims = board.getRecentClaims();
        if (claims == null) {
            add(message(SECRET_MESSAGE));
            return;
        }
        if (claims.isEmpty()) {
            add(message("No claims yet. They show up here as teams claim tiles."));
            return;
        }
        for (int index = 0; index < claims.size(); index++) {
            add(claimRow(claims.get(index), index == claims.size() - 1));
        }
    }

    private JPanel claimRow(PluginClaimData claim, boolean last) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, last ? 0 : 1, 0, Theme.BORDER_SUBTLE),
                new EmptyBorder(6, 0, 6, 0)));
        JLabel dot = new JLabel(new DotIcon(Theme.parseTeamColor(claim.getTeamColor())));
        dot.setVerticalAlignment(JLabel.TOP);
        dot.setBorder(new EmptyBorder(4, 0, 0, 0));
        WrapText text = Components.wrapped(claim.getTeamName() + " claimed " + claim.getTileTitle(),
                Components.CONTENT_WIDTH - 50, 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
        JLabel when = Components.label(TimeText.ago(claim.getClaimedAt()).replace(" ago", ""), 10f, Font.PLAIN,
                Theme.TEXT_MUTED);
        when.setVerticalAlignment(JLabel.TOP);
        row.add(dot, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.add(when, BorderLayout.EAST);
        return fitWidth(row);
    }

    private static WrapText message(String text) {
        return Components.wrapped(text, Components.CONTENT_WIDTH - 24, 11f, Font.PLAIN, Theme.TEXT_MUTED);
    }

    private static JPanel fitWidth(JPanel row) {
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
