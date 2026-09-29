package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import com.lumbridgeguide.gear.data.OwnedGearConfig;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.ProgressBar;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

/** Compares what the player wears and carries with one of their configs, and lists what is missing and where. */
class TripCheckPanel extends Section {

    private final TripCheckService tripCheckService;
    private final GearTagService gearTagService;
    private final Card card;
    private final JButton makeTabButton;
    private final WrapText statusText;
    private OwnedGearConfig config;
    private boolean bankOpen;

    TripCheckPanel(TripCheckService tripCheckService, GearTagService gearTagService, Runnable onBack) {
        super(0);
        this.tripCheckService = tripCheckService;
        this.gearTagService = gearTagService;

        JButton back = Components.linkButton("‹  My configs");
        back.addActionListener(event -> onBack.run());
        JButton recheck = Components.linkButton("↻ Recheck");
        recheck.setForeground(Theme.TEXT_SECONDARY);
        recheck.setFont(Components.font(10f, Font.PLAIN));
        recheck.addActionListener(event -> run());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setAlignmentX(LEFT_ALIGNMENT);
        top.add(back, BorderLayout.WEST);
        top.add(recheck, BorderLayout.EAST);
        top.setMaximumSize(new Dimension(Integer.MAX_VALUE, top.getPreferredSize().height));

        card = new Card(12);

        makeTabButton = Components.fullWidthButton("Make tab in bank");
        makeTabButton.addActionListener(event -> makeTab());
        statusText = GearForm.status(" ");

        add(top);
        add(Box.createVerticalStrut(10));
        add(card);
        add(Box.createVerticalStrut(8));
        add(Components.wrapped("Compares what you wear and carry with the config before you leave the bank.",
                Components.CONTENT_WIDTH, 10f, Font.PLAIN, Theme.TEXT_MUTED));
    }

    void show(OwnedGearConfig newConfig) {
        config = newConfig;
        run();
    }

    void setBankOpen(boolean open) {
        bankOpen = open;
        makeTabButton.setEnabled(open);
    }

    private void run() {
        if (config == null) {
            return;
        }
        card.removeAll();
        card.add(Components.sectionLabel("Trip check"));
        card.add(Box.createVerticalStrut(6));
        card.add(Components.wrapped(config.getName(), GearForm.TEXT_WIDTH, 14f, Font.BOLD, Theme.TEXT_PRIMARY));
        card.add(Box.createVerticalStrut(6));
        card.add(GearForm.status("Checking your items..."));
        refresh();
        tripCheckService.check(config.getId(),
                check -> SwingUtilities.invokeLater(() -> showCheck(check)),
                message -> SwingUtilities.invokeLater(() -> showError(message)));
    }

    private void showCheck(TripCheckService.Check check) {
        card.removeAll();
        card.add(Components.sectionLabel("Trip check"));
        card.add(Box.createVerticalStrut(6));
        card.add(Components.wrapped(check.getName(), GearForm.TEXT_WIDTH, 14f, Font.BOLD, Theme.TEXT_PRIMARY));
        card.add(Box.createVerticalStrut(6));

        JLabel count = Components.monoLabel(String.valueOf(check.have()), 24f, Font.BOLD, Theme.TEXT_PRIMARY);
        JLabel ofTotal = Components.label("/ " + check.total() + " items on you", 12f, Font.PLAIN,
                Theme.TEXT_SECONDARY);
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ((FlowLayout) countRow.getLayout()).setAlignOnBaseline(true);
        countRow.setOpaque(false);
        countRow.setBorder(new EmptyBorder(0, -5, 0, 0));
        countRow.add(count);
        countRow.add(ofTotal);
        addRow(countRow);
        card.add(Box.createVerticalStrut(6));

        ProgressBar bar = new ProgressBar();
        bar.setProgress(check.total() == 0 ? 1 : (double) check.have() / check.total(), Theme.accent());
        card.add(bar);
        card.add(Box.createVerticalStrut(6));

        addTally("Worn", check.getWornHave(), check.getWornTotal(), true);
        addTally("Inventory", check.getInventoryHave(), check.getInventoryTotal(), true);
        addTally("Rune pouch", check.getPouchHave(), check.getPouchTotal(), false);

        if (!check.getMissing().isEmpty()) {
            card.add(Box.createVerticalStrut(10));
            card.add(Components.sectionLabel("Missing  ·  " + check.getMissing().size()));
            card.add(Box.createVerticalStrut(4));
            for (TripCheckService.Missing missing : check.getMissing()) {
                addMissing(missing);
            }
        }

        card.add(Box.createVerticalStrut(10));
        card.add(makeTabButton);
        card.add(Box.createVerticalStrut(6));
        statusText.setText(bankOpen ? " " : "Open your bank to make the tab.");
        statusText.setForeground(Theme.TEXT_MUTED);
        card.add(statusText);
        makeTabButton.setEnabled(bankOpen);
        refresh();
    }

    private void addTally(String name, int have, int total, boolean ruled) {
        if (total == 0) {
            return;
        }
        Color tone = have == total ? Theme.SUCCESS : have == 0 ? Theme.ERROR : Theme.WARNING;
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, ruled ? 1 : 0, 0, Theme.BORDER_SUBTLE),
                new EmptyBorder(5, 0, 5, 0)));
        JLabel label = Components.label(name, 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
        label.setIcon(new DotIcon(tone));
        label.setIconTextGap(6);
        JLabel value = Components.monoLabel(have + " / " + total, 11f, Font.PLAIN, Theme.TEXT_SECONDARY);
        row.add(label, BorderLayout.WEST);
        row.add(value, BorderLayout.EAST);
        addRow(row);
    }

    private void addMissing(TripCheckService.Missing missing) {
        String where;
        Color tone;
        switch (missing.getWhereabouts()) {
            case IN_BANK:
                where = "In bank";
                tone = Theme.TEXT_SECONDARY;
                break;
            case NOT_OWNED:
                where = "Not owned";
                tone = Theme.ERROR;
                break;
            default:
                where = "Not carried";
                tone = Theme.WARNING;
                break;
        }
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        String name = missing.getCount() > 1 ? missing.getName() + " x" + missing.getCount() : missing.getName();
        row.add(Components.label(name, 11f, Font.PLAIN, Theme.TEXT_PRIMARY), BorderLayout.CENTER);
        row.add(Components.label(where, 10f, Font.PLAIN, tone), BorderLayout.EAST);
        addRow(row);
    }

    private void showError(String message) {
        card.removeAll();
        card.add(Components.sectionLabel("Trip check"));
        card.add(Box.createVerticalStrut(6));
        WrapText error = GearForm.status(message);
        error.setForeground(Theme.ERROR);
        card.add(error);
        refresh();
    }

    private void makeTab() {
        if (config == null || !bankOpen) {
            return;
        }
        makeTabButton.setEnabled(false);
        makeTabButton.setText("Making...");
        gearTagService.generate(config.getId(), true, result -> SwingUtilities.invokeLater(() -> {
            makeTabButton.setEnabled(bankOpen);
            makeTabButton.setText("Make tab in bank");
            statusText.setText(result.getMessage());
            statusText.setForeground(result.isSuccess() ? Theme.SUCCESS : Theme.ERROR);
            refresh();
        }));
    }

    private void addRow(JPanel row) {
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        card.add(row);
    }

    private void refresh() {
        revalidate();
        repaint();
    }
}
