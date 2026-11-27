package com.lumbridgeguide.ui;

import com.lumbridgeguide.account.ui.AccountFooter;
import com.lumbridgeguide.api.LumbridgeGuideClient;
import com.lumbridgeguide.bingo.ui.BingoTabPanel;
import com.lumbridgeguide.gear.ui.GearTabPanel;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;

public class SidebarPanel extends PluginPanel {

    private static final String NO_KEY_CARD = "noKey";
    private static final String TABS_CARD = "tabs";
    private static final String BINGO_TAB = "Bingo";
    private static final String GEAR_TAB = "Gear";

    private final LumbridgeGuideClient apiClient;
    private final JPanel centerPanel;
    private final BingoTabPanel bingoTab;
    private final GearTabPanel gearTab;
    private final AccountFooter accountFooter;

    @Inject
    public SidebarPanel(BingoTabPanel bingoTab, GearTabPanel gearTab, AccountFooter accountFooter,
                        LumbridgeGuideClient apiClient) {
        super(false);
        this.bingoTab = bingoTab;
        this.gearTab = gearTab;
        this.accountFooter = accountFooter;
        this.apiClient = apiClient;

        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        setBackground(Theme.PANEL_BACKGROUND);

        JLabel pluginTitle = Components.label("Lumbridge Guide", Components.BOLD, Theme.TEXT_PRIMARY);
        pluginTitle.setBorder(new EmptyBorder(2, 0, 10, 0));
        add(pluginTitle, BorderLayout.NORTH);

        centerPanel = new JPanel(new CardLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(buildNoKeyCard(), NO_KEY_CARD);
        centerPanel.add(buildTabsCard(), TABS_CARD);
        add(centerPanel, BorderLayout.CENTER);
        add(accountFooter, BorderLayout.SOUTH);

        refresh();
    }

    public void setBankOpen(boolean open) {
        SwingUtilities.invokeLater(() -> gearTab.setBankOpen(open));
    }

    public void refresh() {
        SwingUtilities.invokeLater(() -> {
            boolean hasKey = apiClient.hasApiKey();

            ((CardLayout) centerPanel.getLayout()).show(centerPanel, hasKey ? TABS_CARD : NO_KEY_CARD);
            accountFooter.setAccountVisible(hasKey);

            if (hasKey) {
                bingoTab.refresh();
            }

            revalidate();
            repaint();
        });
    }

    private JPanel buildTabsCard() {
        CardLayout tabCards = new CardLayout();
        JPanel display = new JPanel(tabCards);
        display.setOpaque(false);
        display.add(bingoTab, BINGO_TAB);
        display.add(gearTab, GEAR_TAB);

        TabBar tabBar = new TabBar(List.of(BINGO_TAB, GEAR_TAB),
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

        JLabel title = Components.label("Plugin key needed", Components.BOLD, Theme.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(6));
        WrapText message = Components.wrapped("Add your plugin key in the plugin settings",
                Components.CONTENT_WIDTH - 12, Components.REGULAR, Theme.TEXT_SECONDARY);
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
                    Components.SMALL, Theme.TEXT_MUTED);
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
