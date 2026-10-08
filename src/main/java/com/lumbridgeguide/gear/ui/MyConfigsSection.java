package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.data.OwnedGearConfig;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;
import com.lumbridgeguide.ui.Theme;
import com.lumbridgeguide.ui.TimeText;
import com.lumbridgeguide.ui.WrapText;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * The player's own gear configs from the website, listed like the bingo tiles. Clicking one opens its trip check, where
 * its bank tag tab is made. Importing someone else's config by its code stays on the Bank tab page.
 */
class MyConfigsSection extends Section {

    private final GearTagService gearTagService;
    private final Consumer<OwnedGearConfig> onOpen;
    private final JLabel countLabel;
    private final JTextField filterField;
    private final JPanel list;
    private final WrapText messageText;
    private final List<ConfigRow> rows = new ArrayList<>();
    private boolean loaded;

    MyConfigsSection(GearTagService gearTagService, Consumer<OwnedGearConfig> onOpen) {
        super(0);
        this.gearTagService = gearTagService;
        this.onOpen = onOpen;

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

        list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        list.setAlignmentX(LEFT_ALIGNMENT);

        messageText = GearForm.status(" ");

        WrapText help = Components.wrapped("Configs you own on the website. Click one to check what you carry "
                + "against it and make its bank tab. For anyone else's config, paste its code on the Bank tab page.",
                Components.CONTENT_WIDTH, 10f, Font.PLAIN, Theme.TEXT_MUTED);

        add(header);
        add(Box.createVerticalStrut(8));
        add(filterField);
        add(Box.createVerticalStrut(6));
        add(list);
        add(messageText);
        add(Box.createVerticalStrut(8));
        add(help);
    }

    /** Loads the list the first time the subtab is opened, so players who never use it cost no request. */
    void loadIfNeeded() {
        if (!loaded) {
            load();
        }
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

    private static String detailText(OwnedGearConfig config) {
        String when = TimeText.ago(config.getUpdatedAt());
        return config.getItemCount() + " items" + (when.isEmpty() ? "" : "  ·  " + when);
    }

    /** One config, drawn like a bingo tile row: its name, item count and last change over a hairline rule. */
    private final class ConfigRow extends JPanel {

        private final OwnedGearConfig config;

        private ConfigRow(OwnedGearConfig config) {
            super(new BorderLayout(7, 0));
            this.config = config;
            setBackground(Theme.SURFACE_RAISED);
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER_SUBTLE), new EmptyBorder(7, 2, 7, 2)));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            JLabel name = Components.label(config.getName(), 12f, Font.PLAIN, Theme.TEXT_PRIMARY);
            JLabel detail = Components.label(detailText(config), 10f, Font.PLAIN, Theme.TEXT_MUTED);
            detail.setBorder(new EmptyBorder(1, 0, 0, 0));
            text.add(name);
            text.add(detail);

            add(text, BorderLayout.CENTER);
            add(Components.label("›", 13f, Font.PLAIN, Theme.TEXT_MUTED), BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent mouseEvent) {
                    setOpaque(true);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent mouseEvent) {
                    setOpaque(false);
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    onOpen.accept(config);
                }
            });
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }
}
