package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.bingo.data.PluginBoardData;
import com.lumbridgeguide.bingo.data.PluginClaimData;
import com.lumbridgeguide.bingo.data.PluginStandingData;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.SwatchIcon;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * The board's standings or its latest claims, depending on {@link Mode}, laid out like the website's board page.
 * Hidden boards send neither while their claims are secret, so this says so instead.
 */
class StandingsPanel extends Section {

    enum Mode {
        STANDINGS,
        CLAIMS
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
                showClaims(board);
            }
        }
        revalidate();
        repaint();
    }

    private void showStandings(PluginBoardData board) {
        int tiles = board.getTiles() == null ? 0 : board.getTiles().size();
        add(Components.sectionHeader("Standings", Components.label(tiles + " tiles", 11f, Font.PLAIN,
                Theme.TEXT_MUTED)));
        add(Box.createVerticalStrut(6));
        List<PluginStandingData> standings = board.getStandings();
        if (standings == null) {
            add(message(SECRET_MESSAGE));
            return;
        }
        int top = standings.stream().mapToInt(PluginStandingData::getScore).max().orElse(0);
        String myTeamId = board.getMyTeam() == null ? null : board.getMyTeam().getId();
        for (PluginStandingData standing : standings) {
            add(standingRow(standing, top, board.isTilePointsEnabled(), standing.getTeamId().equals(myTeamId)));
        }
    }

    private JPanel standingRow(PluginStandingData standing, int top, boolean points, boolean mine) {
        Color colour = Theme.parseTeamColor(standing.getColor());
        JPanel row = new JPanel(new BorderLayout(0, 6));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 7, 0));

        boolean first = standing.getRank() == 1;
        JLabel rank = Components.monoLabel(String.valueOf(standing.getRank()), 11f, Font.BOLD,
                first ? Theme.accentText() : Theme.TEXT_MUTED);
        JLabel name = Components.label(standing.getName(), 12f, mine ? Font.BOLD : Font.PLAIN,
                first || mine ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY);
        name.setIcon(new SwatchIcon(colour));
        name.setIconTextGap(6);
        String unit = points ? " pts" : standing.getScore() == 1 ? " tile" : " tiles";
        JLabel score = Components.label(standing.getScore() + unit, 12f, Font.BOLD, Theme.TEXT_PRIMARY);

        JPanel line = new JPanel(new BorderLayout(4, 0));
        line.setOpaque(false);
        line.add(rank, BorderLayout.WEST);
        line.add(name, BorderLayout.CENTER);
        line.add(score, BorderLayout.EAST);

        ProgressBar progress = new ProgressBar(2);
        progress.setProgress(top == 0 ? 0 : (double) standing.getScore() / top, colour);

        row.add(line, BorderLayout.NORTH);
        row.add(progress, BorderLayout.SOUTH);
        return fitWidth(row);
    }

    private void showClaims(PluginBoardData board) {
        add(Components.sectionHeader("Latest claims"));
        add(Box.createVerticalStrut(2));
        List<PluginClaimData> claims = board.getRecentClaims();
        if (claims == null) {
            add(message(SECRET_MESSAGE));
            return;
        }
        if (claims.isEmpty()) {
            add(message("No claims yet. They show up here as teams claim tiles."));
            return;
        }
        for (PluginClaimData claim : claims) {
            add(claimRow(claim));
        }
    }

    /** A claim as the website lists it: when, the team's colour, the tile, then the team. */
    private JPanel claimRow(PluginClaimData claim) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(7, 0, 7, 0)));

        JLabel when = Components.monoLabel(TimeText.clock(claim.getClaimedAt()), 10f, Font.PLAIN, Theme.TEXT_MUTED);
        when.setVerticalAlignment(JLabel.TOP);
        when.setPreferredSize(new Dimension(40, when.getPreferredSize().height));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel tile = Components.label(claim.getTileTitle(), 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
        tile.setIcon(new SwatchIcon(Theme.parseTeamColor(claim.getTeamColor())));
        tile.setIconTextGap(6);
        JLabel team = Components.label(claim.getTeamName(), 11f, Font.PLAIN, Theme.TEXT_MUTED);
        team.setBorder(new EmptyBorder(1, 14, 0, 0));
        text.add(tile);
        text.add(team);

        row.add(when, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return fitWidth(row);
    }

    private static WrapText message(String text) {
        return Components.wrapped(text, Components.CONTENT_WIDTH - 12, 11f, Font.PLAIN, Theme.TEXT_MUTED);
    }

    private static JPanel fitWidth(JPanel row) {
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }
}
