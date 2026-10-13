package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.TabBar;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Arrays;

/** Sidebar page for gear bank tags, the player's own configs and new configs */
public class GearTabPanel extends JPanel {

    private static final String BANK_TAB = "Bank tab";
    private static final String MY_CONFIGS = "My configs";
    private static final String CREATE_CONFIG = "Create";
    private static final String TRIP_CHECK = "tripCheck";

    private final BankTabSection bankTabSection;
    private final MyConfigsSection myConfigsSection;
    private final TripCheckPanel tripCheckPanel;

    public GearTabPanel(GearTagService gearTagService, GearConfigExportService gearConfigExportService,
                        TripCheckService tripCheckService, boolean includeMissingByDefault) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        bankTabSection = new BankTabSection(gearTagService, includeMissingByDefault);
        CardLayout cards = new CardLayout();
        JPanel display = new JPanel(cards);
        tripCheckPanel = new TripCheckPanel(tripCheckService, gearTagService, () -> cards.show(display, MY_CONFIGS));
        myConfigsSection = new MyConfigsSection(gearTagService, config -> {
            tripCheckPanel.show(config);
            cards.show(display, TRIP_CHECK);
        });

        display.setOpaque(false);
        display.add(topAligned(bankTabSection), BANK_TAB);
        display.add(Components.scrolling(topAligned(myConfigsSection)), MY_CONFIGS);
        display.add(topAligned(new CreateConfigSection(gearConfigExportService)), CREATE_CONFIG);
        display.add(Components.scrolling(topAligned(tripCheckPanel)), TRIP_CHECK);

        TabBar subtabs = new TabBar(Arrays.asList(BANK_TAB, MY_CONFIGS, CREATE_CONFIG), TabBar.Style.PILL, name -> {
            cards.show(display, name);
            if (MY_CONFIGS.equals(name)) {
                myConfigsSection.loadIfNeeded();
            }
        });
        subtabs.setBorder(new EmptyBorder(0, 0, 10, 0));

        add(subtabs, BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
    }

    public void setBankOpen(boolean open) {
        bankTabSection.setBankOpen(open);
        myConfigsSection.setBankOpen(open);
        tripCheckPanel.setBankOpen(open);
    }

    /** CardLayout stretches each card to fill the space, so each section sits at the top of its own panel. */
    private static JPanel topAligned(JPanel section) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(section, BorderLayout.NORTH);
        return wrapper;
    }
}
