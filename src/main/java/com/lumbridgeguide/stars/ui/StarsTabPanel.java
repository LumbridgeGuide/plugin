package com.lumbridgeguide.stars.ui;

import com.lumbridgeguide.stars.ShootingStarData;
import com.lumbridgeguide.stars.ShootingStarService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;
import net.runelite.api.coords.WorldPoint;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Comparator;
import java.util.List;

/** The star next to the player, ready to report, and the live stars other players have reported. */
public class StarsTabPanel extends Section {

    private final ShootingStarService starService;
    private final Section nearbyCard;
    private final JLabel listLabel;
    private final Section list;
    private boolean loaded;

    public StarsTabPanel(ShootingStarService starService) {
        super(0);
        this.starService = starService;

        nearbyCard = new Section(0);
        nearbyCard.setVisible(false);

        listLabel = Components.label("", 11f, Font.PLAIN, Theme.TEXT_MUTED);
        JButton refresh = Components.textLink("Refresh");
        refresh.addActionListener(event -> load());
        JPanel trailing = new JPanel();
        trailing.setLayout(new BoxLayout(trailing, BoxLayout.X_AXIS));
        trailing.setOpaque(false);
        trailing.add(listLabel);
        trailing.add(Box.createHorizontalStrut(8));
        trailing.add(refresh);
        JPanel header = Components.sectionHeader("Live stars", trailing);

        list = new Section(0);

        add(nearbyCard);
        add(Box.createVerticalStrut(12));
        add(header);
        add(Box.createVerticalStrut(4));
        add(list);
        add(Box.createVerticalStrut(8));
        add(Components.wrapped("Stars you report also show on the website map.", Components.CONTENT_WIDTH, 11f,
                Font.PLAIN, Theme.TEXT_MUTED));

        starService.setOnNearbyStar(star -> SwingUtilities.invokeLater(() -> showNearby(star)));
    }

    /** Loads the list the first time the tab opens, so players who never look cost no request. */
    public void loadIfNeeded() {
        if (!loaded) {
            load();
        }
    }

    private void load() {
        loaded = true;
        list.removeAll();
        list.add(message("Loading stars..."));
        refresh();
        starService.list(
                stars -> SwingUtilities.invokeLater(() -> showStars(stars)),
                failure -> SwingUtilities.invokeLater(() -> {
                    list.removeAll();
                    list.add(message(failure));
                    refresh();
                }));
    }

    private void showNearby(ShootingStarData star) {
        nearbyCard.removeAll();
        nearbyCard.setVisible(star != null);
        if (star != null) {
            nearbyCard.add(Components.sectionHeader("Star next to you"));
            nearbyCard.add(Box.createVerticalStrut(8));

            JPanel summary = new JPanel(new BorderLayout(8, 0));
            summary.setOpaque(false);
            summary.setAlignmentX(LEFT_ALIGNMENT);
            summary.add(Components.monoLabel(String.valueOf(star.getLevel()), 24f, Font.BOLD, Theme.TEXT_PRIMARY),
                    BorderLayout.WEST);
            JPanel text = new JPanel(new BorderLayout());
            text.setOpaque(false);
            text.add(Components.label("Size " + star.getLevel() + "  ·  World " + star.getWorld(), 12f, Font.PLAIN,
                    Theme.TEXT_PRIMARY), BorderLayout.NORTH);
            text.add(Components.label(star.getX() + ", " + star.getY(), 10f, Font.PLAIN, Theme.TEXT_MUTED),
                    BorderLayout.SOUTH);
            summary.add(text, BorderLayout.CENTER);
            summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, summary.getPreferredSize().height));
            nearbyCard.add(summary);
            nearbyCard.add(Box.createVerticalStrut(8));

            WrapText result = Components.wrapped("Report it so others can find it.", Components.CONTENT_WIDTH,
                    11f, Font.PLAIN, Theme.TEXT_MUTED);
            JButton report = Components.fullWidthButton("Report star");
            report.addActionListener(event -> {
                report.setEnabled(false);
                starService.report(star, message -> SwingUtilities.invokeLater(() -> {
                    result.setText(message);
                    report.setText("Reported");
                    load();
                }));
            });
            nearbyCard.add(result);
            nearbyCard.add(Box.createVerticalStrut(8));
            nearbyCard.add(report);
        }
        refresh();
    }

    private void showStars(List<ShootingStarData> stars) {
        list.removeAll();
        listLabel.setText(String.valueOf(stars.size()));
        if (stars.isEmpty()) {
            list.add(message("No stars reported right now."));
        }
        stars.stream()
                .sorted(Comparator.comparingInt(ShootingStarData::getLevel).reversed())
                .forEach(star -> list.add(row(star)));
        refresh();
    }

    private JPanel row(ShootingStarData star) {
        JPanel row = new JPanel(new BorderLayout(9, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(7, 0, 7, 0)));
        JLabel size = Components.monoLabel(String.valueOf(star.getLevel()), 13f, Font.BOLD, Theme.accentText());
        size.setHorizontalAlignment(SwingConstants.CENTER);
        size.setPreferredSize(new Dimension(20, 24));
        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(Components.label("World " + star.getWorld(), 12f, Font.PLAIN, Theme.TEXT_PRIMARY),
                BorderLayout.NORTH);
        text.add(Components.label(details(star), 11f, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.SOUTH);
        row.add(size, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private String details(ShootingStarData star) {
        String detail = "Size " + star.getLevel() + "  ·  " + TimeText.ago(star.getReportedAt());
        WorldPoint player = starService.getPlayerLocation();
        if (player != null && player.getPlane() == star.getPlane()) {
            int distance = player.distanceTo2D(new WorldPoint(star.getX(), star.getY(), star.getPlane()));
            detail += "  ·  " + distance + " tiles";
        }
        return detail;
    }

    private static WrapText message(String text) {
        return Components.wrapped(text, Components.CONTENT_WIDTH, 11f, Font.PLAIN, Theme.TEXT_MUTED);
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}
