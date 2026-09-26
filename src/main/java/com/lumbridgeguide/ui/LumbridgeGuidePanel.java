package com.lumbridgeguide.ui;

import com.lumbridgeguide.LumbridgeGuideConfig;
import com.lumbridgeguide.service.AccountSyncService;
import com.lumbridgeguide.service.BoardDataService;
import com.lumbridgeguide.service.GearConfigExportService;
import com.lumbridgeguide.service.GearTagService;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;

public class LumbridgeGuidePanel extends PluginPanel {

    private static final String NO_KEY_CARD = "noKey";
    private static final String TABS_CARD = "tabs";
    private static final String BINGO_TAB = "Bingo";
    private static final String GEAR_TAB = "Gear";
    private static final String ACCOUNT_TAB = "Account";

    private final LumbridgeGuideConfig config;
    private final JPanel centerPanel;
    private final BingoTabPanel bingoTab;

    public LumbridgeGuidePanel(
            BoardDataService boardDataService,
            GearTagService gearTagService,
            GearConfigExportService gearConfigExportService,
            AccountSyncService accountSyncService,
            ItemManager itemManager,
            SkillIconManager skillIconManager,
            LumbridgeGuideConfig config) {
        super(false);
        this.config = config;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(LumbridgeGuideTheme.PANEL_BACKGROUND);

        JLabel pluginTitle = Ui.label("Lumbridge Guide", 16f, Font.BOLD, LumbridgeGuideTheme.TEXT_PRIMARY);
        pluginTitle.setHorizontalAlignment(SwingConstants.CENTER);
        pluginTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        add(pluginTitle, BorderLayout.NORTH);

        bingoTab = new BingoTabPanel(boardDataService, config, itemManager, skillIconManager);

        centerPanel = new JPanel(new CardLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(buildNoKeyCard(), NO_KEY_CARD);
        centerPanel.add(buildTabsCard(
                bingoTab,
                new GearTabPanel(gearTagService, gearConfigExportService),
                new AccountTabPanel(accountSyncService)), TABS_CARD);
        add(centerPanel, BorderLayout.CENTER);

        refresh();
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

    private static JPanel buildTabsCard(BingoTabPanel bingoTab, GearTabPanel gearTab, AccountTabPanel accountTab) {
        CardLayout tabCards = new CardLayout();
        JPanel display = new JPanel(tabCards);
        display.setOpaque(false);
        display.add(bingoTab, BINGO_TAB);
        display.add(gearTab, GEAR_TAB);
        display.add(accountTab, ACCOUNT_TAB);

        ThemedTabBar tabBar = new ThemedTabBar(Arrays.asList(BINGO_TAB, GEAR_TAB, ACCOUNT_TAB),
                name -> tabCards.show(display, name));
        tabBar.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.add(tabBar, BorderLayout.NORTH);
        card.add(display, BorderLayout.CENTER);
        return card;
    }

    private static JPanel buildNoKeyCard() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        JLabel title = Ui.label("API key needed", 14f, Font.BOLD, LumbridgeGuideTheme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(30, 0, 8, 0));

        JComponent message = Ui.wrapped("Set your API key in the plugin settings to get started.",
                Ui.CONTENT_WIDTH - 20, 12f, Font.PLAIN, LumbridgeGuideTheme.TEXT_SECONDARY);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(message);
        return panel;
    }
}
