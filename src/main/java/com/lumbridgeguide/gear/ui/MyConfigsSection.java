package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.data.OwnedGearConfig;
import com.lumbridgeguide.ui.Card;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.DotIcon;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The player's own gear configs from the website, each one click away from a bank tag tab. Importing someone else's
 * config by its code stays on the Bank tab page.
 */
class MyConfigsSection extends Section {

    private final GearTagService gearTagService;
    private final Consumer<OwnedGearConfig> onCheck;
    private final JPanel bankNotice;
    private final JLabel countLabel;
    private final JTextField filterField;
    private final JCheckBox includeMissingBox;
    private final JPanel list;
    private final WrapText messageText;
    private final List<ConfigRow> rows = new ArrayList<>();
    private boolean bankOpen;
    private boolean loaded;
    private boolean making;

    MyConfigsSection(GearTagService gearTagService, boolean includeMissingByDefault,
                     Consumer<OwnedGearConfig> onCheck) {
        super(0);
        this.gearTagService = gearTagService;
        this.onCheck = onCheck;

        bankNotice = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(getBackground());
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        bankNotice.setOpaque(false);
        bankNotice.setBackground(new Color(Theme.WARNING.getRed(), Theme.WARNING.getGreen(),
                Theme.WARNING.getBlue(), 26));
        bankNotice.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.WARNING.darker()), new EmptyBorder(7, 9, 7, 9)));
        bankNotice.add(Components.statusLine("Open your bank to make a tab.", Theme.WARNING, Theme.TEXT_PRIMARY));
        bankNotice.setAlignmentX(LEFT_ALIGNMENT);
        bankNotice.setMaximumSize(new Dimension(Integer.MAX_VALUE, bankNotice.getPreferredSize().height));

        countLabel = Components.sectionLabel("Your configs");
        JButton refreshLink = Components.linkButton("↻ Refresh");
        refreshLink.setForeground(Theme.TEXT_SECONDARY);
        refreshLink.setFont(Components.font(10f, Font.PLAIN));
        refreshLink.addActionListener(event -> load());
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(LEFT_ALIGNMENT);
        header.add(countLabel, BorderLayout.WEST);
        header.add(refreshLink, BorderLayout.EAST);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, header.getPreferredSize().height));

        filterField = GearForm.textField("Filter configs");
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilter();
            }
        });

        includeMissingBox = GearForm.checkBox("Include missing items");
        includeMissingBox.setSelected(includeMissingByDefault);

        list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setAlignmentX(LEFT_ALIGNMENT);

        messageText = GearForm.status(" ");

        Card card = new Card(12);
        card.add(header);
        card.add(Box.createVerticalStrut(8));
        card.add(filterField);
        card.add(Box.createVerticalStrut(8));
        card.add(includeMissingBox);
        card.add(Box.createVerticalStrut(4));
        card.add(list);
        card.add(messageText);

        WrapText help = Components.wrapped("Configs you own on the website. Click one to check what you carry "
                + "against it. For anyone else's config, paste its code on the Bank tab page.",
                Components.CONTENT_WIDTH, 10f, Font.PLAIN, Theme.TEXT_MUTED);

        add(bankNotice);
        add(Box.createVerticalStrut(10));
        add(card);
        add(Box.createVerticalStrut(8));
        add(help);
        setBankOpen(false);
    }

    /** Loads the list the first time the subtab is opened, so players who never use it cost no request. */
    void loadIfNeeded() {
        if (!loaded) {
            load();
        }
    }

    void setBankOpen(boolean open) {
        bankOpen = open;
        bankNotice.setVisible(!open);
        rows.forEach(ConfigRow::refreshEnabled);
        revalidate();
        repaint();
    }

    private void load() {
        loaded = true;
        showMessage("Loading your configs...");
        gearTagService.listOwnConfigs(
                configs -> SwingUtilities.invokeLater(() -> showConfigs(configs)),
                message -> SwingUtilities.invokeLater(() -> showMessage(message)));
    }

    private void showConfigs(List<OwnedGearConfig> configs) {
        rows.clear();
        list.removeAll();
        for (OwnedGearConfig config : configs) {
            ConfigRow row = new ConfigRow(config);
            rows.add(row);
            list.add(row);
        }
        countLabel.setText(("Your configs  ·  " + configs.size()).toUpperCase(Locale.ROOT));
        showMessage(configs.isEmpty()
                ? "No gear configs yet. Make one on the website or from the Create config page."
                : null);
        applyFilter();
    }

    private void applyFilter() {
        String query = filterField.getText().trim().toLowerCase(Locale.ROOT);
        for (ConfigRow row : rows) {
            row.setVisible(query.isEmpty() || row.config.getName().toLowerCase(Locale.ROOT).contains(query));
        }
        list.revalidate();
        list.repaint();
    }

    private void showMessage(String message) {
        messageText.setText(message == null ? "" : message);
        messageText.setVisible(message != null);
        revalidate();
        repaint();
    }

    static String ago(String isoInstant) {
        try {
            Duration age = Duration.between(Instant.parse(isoInstant), Instant.now());
            if (age.toDays() >= 30) {
                return age.toDays() / 30 + "mo ago";
            }
            if (age.toDays() >= 7) {
                return age.toDays() / 7 + "w ago";
            }
            if (age.toDays() >= 1) {
                return age.toDays() + "d ago";
            }
            if (age.toHours() >= 1) {
                return age.toHours() + "h ago";
            }
            return "just now";
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    /** One config: its name, how many items and when it changed, and the button that makes its tab. */
    private final class ConfigRow extends JPanel {

        private final OwnedGearConfig config;
        private final JLabel detailLabel;
        private final JButton makeButton;

        private ConfigRow(OwnedGearConfig config) {
            super(new BorderLayout(8, 0));
            this.config = config;
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(7, 1, 7, 1)));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            JLabel name = Components.label(config.getName(), 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
            detailLabel = Components.label(defaultDetail(), 10f, Font.PLAIN, Theme.TEXT_MUTED);
            text.add(name);
            text.add(Box.createVerticalStrut(1));
            text.add(detailLabel);
            text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            text.setToolTipText("Trip check: compare what you wear and carry with this config");
            text.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    onCheck.accept(config);
                }
            });

            makeButton = Components.rowButton("Make tab");
            makeButton.addActionListener(event -> make());
            JPanel buttonHolder = new JPanel(new BorderLayout());
            buttonHolder.setOpaque(false);
            buttonHolder.add(makeButton, BorderLayout.CENTER);

            add(text, BorderLayout.CENTER);
            add(buttonHolder, BorderLayout.EAST);
            refreshEnabled();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        private String defaultDetail() {
            String when = ago(config.getUpdatedAt());
            return config.getItemCount() + " items" + (when.isEmpty() ? "" : "  ·  " + when);
        }

        private void refreshEnabled() {
            makeButton.setEnabled(bankOpen && !making);
        }

        private void make() {
            if (!bankOpen || making) {
                return;
            }
            making = true;
            makeButton.setText("Making...");
            rows.forEach(ConfigRow::refreshEnabled);
            gearTagService.generate(config.getId(), includeMissingBox.isSelected(),
                    result -> SwingUtilities.invokeLater(() -> finish(result)));
        }

        private void finish(GearTagService.Result result) {
            making = false;
            makeButton.setText(result.isSuccess() ? "Remake" : "Make tab");
            rows.forEach(ConfigRow::refreshEnabled);
            if (!result.isSuccess()) {
                showDetail(result.getMessage(), Theme.ERROR, null);
                return;
            }
            List<String> missing = result.getMissingItems();
            if (missing.isEmpty()) {
                showDetail("Tab made", Theme.SUCCESS, null);
            } else {
                showDetail("Tab made  ·  " + missing.size() + " missing", Theme.SUCCESS,
                        "Not in your bank: " + String.join(", ", missing));
            }
        }

        private void showDetail(String text, Color colour, String tooltip) {
            detailLabel.setText(text);
            detailLabel.setForeground(colour);
            detailLabel.setIcon(colour == Theme.ERROR ? new DotIcon(colour) : null);
            detailLabel.setToolTipText(tooltip);
            revalidate();
        }
    }
}
