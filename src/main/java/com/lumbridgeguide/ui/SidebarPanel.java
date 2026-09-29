package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.account.AccountSyncService;
import com.lumbridgeguide.account.ui.AccountTabPanel;
import com.lumbridgeguide.bingo.BoardDataService;
import com.lumbridgeguide.bingo.TileProgressTracker;
import com.lumbridgeguide.bingo.ui.BingoTabPanel;
import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import com.lumbridgeguide.gear.ui.GearTabPanel;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;

public class SidebarPanel extends PluginPanel {

    private static final String NO_KEY_CARD = "noKey";
    private static final String TABS_CARD = "tabs";
    private static final String BINGO_TAB = "Bingo";
    private static final String GEAR_TAB = "Gear";
    private static final String ACCOUNT_TAB = "Account";

    private final LumbridgeGuideConfig config;
    private final JPanel centerPanel;
    private final BingoTabPanel bingoTab;
    private final GearTabPanel gearTab;

    public SidebarPanel(
            BoardDataService boardDataService,
            TileProgressTracker tileProgressTracker,
            GearTagService gearTagService,
            GearConfigExportService gearConfigExportService,
            TripCheckService tripCheckService,
            AccountSyncService accountSyncService,
            ItemManager itemManager,
            SkillIconManager skillIconManager,
            LumbridgeGuideConfig config) {
        super(false);
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(Theme.PANEL_BACKGROUND);

        JLabel pluginTitle = Components.label("Lumbridge Guide", 16f, Font.BOLD, Theme.TEXT_PRIMARY);
        pluginTitle.setBorder(new EmptyBorder(2, 0, 10, 0));
        add(pluginTitle, BorderLayout.NORTH);

        bingoTab = new BingoTabPanel(boardDataService, tileProgressTracker, config, itemManager, skillIconManager);
        gearTab = new GearTabPanel(gearTagService, gearConfigExportService, tripCheckService,
                config.includeMissingItems());

        centerPanel = new JPanel(new CardLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(buildNoKeyCard(), NO_KEY_CARD);
        centerPanel.add(buildTabsCard(new AccountTabPanel(accountSyncService)), TABS_CARD);
        add(centerPanel, BorderLayout.CENTER);

        refresh();
    }

    public void setBankOpen(boolean open) {
        SwingUtilities.invokeLater(() -> gearTab.setBankOpen(open));
    }

    public void refresh() {
        SwingUtilities.invokeLater(() ->
        {
            boolean hasKey = config.apiKey() != null && !config.apiKey().trim().isEmpty();

            ((CardLayout) centerPanel.getLayout()).show(centerPanel, hasKey ? TABS_CARD : NO_KEY_CARD);

            if (hasKey) {
                bingoTab.refresh();
            }

            revalidate();
            repaint();
        });
    }

    private JPanel buildTabsCard(AccountTabPanel accountTab) {
        CardLayout tabCards = new CardLayout();
        JPanel display = new JPanel(tabCards);
        display.setOpaque(false);
        display.add(bingoTab, BINGO_TAB);
        display.add(gearTab, GEAR_TAB);
        display.add(accountTab, ACCOUNT_TAB);

        TabBar tabBar = new TabBar(Arrays.asList(BINGO_TAB, GEAR_TAB, ACCOUNT_TAB), TabBar.Style.UNDERLINE,
                name -> tabCards.show(display, name));
        tabBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.add(tabBar, BorderLayout.NORTH);
        card.add(display, BorderLayout.CENTER);
        return card;
    }

    private static JPanel buildNoKeyCard() {
        Section card = new Section(0);

        JLabel title = Components.label("API key needed", 15f, Font.BOLD, Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        WrapText message = Components.wrapped("Set your API key in the plugin settings to get started.",
                Components.CONTENT_WIDTH - 12, 12f, Font.PLAIN, Theme.TEXT_SECONDARY);
        message.setCentred(true);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(message);
        card.add(Box.createVerticalStrut(10));

        String[] steps = {
            "Sign in on the Lumbridge Guide website",
            "Settings, then Account: copy your plugin key",
            "Paste it into this plugin's settings",
        };
        for (int index = 0; index < steps.length; index++) {
            WrapText step = Components.wrapped((index + 1) + ".  " + steps[index], Components.CONTENT_WIDTH - 12,
                    11f, Font.PLAIN, Theme.TEXT_MUTED);
            step.setAlignmentX(Component.CENTER_ALIGNMENT);
            card.add(step);
            card.add(Box.createVerticalStrut(3));
        }

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);
        return wrapper;
    }
}
