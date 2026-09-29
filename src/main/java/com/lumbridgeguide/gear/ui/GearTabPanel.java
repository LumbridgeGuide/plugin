package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.gear.TripCheckService;
import com.lumbridgeguide.ui.TabBar;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Arrays;

/**
 * The Gear tab: "Bank tab" makes a tag tab from any config's code, "My configs" makes one from the player's own configs
 * in a click, and "Create config" exports what they wear and carry.
 */
public class GearTabPanel extends JPanel {

    private static final String BANK_TAB = "Bank tab";
    private static final String MY_CONFIGS = "My configs";
    private static final String CREATE_CONFIG = "Create config";
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
        myConfigsSection = new MyConfigsSection(gearTagService, includeMissingByDefault, config -> {
            tripCheckPanel.show(config);
            cards.show(display, TRIP_CHECK);
        });

        display.setOpaque(false);
        display.add(topAligned(bankTabSection), BANK_TAB);
        display.add(scrolling(topAligned(myConfigsSection)), MY_CONFIGS);
        display.add(topAligned(new CreateConfigSection(gearConfigExportService)), CREATE_CONFIG);
        display.add(scrolling(topAligned(tripCheckPanel)), TRIP_CHECK);

        TabBar subtabs = new TabBar(Arrays.asList(BANK_TAB, MY_CONFIGS, CREATE_CONFIG), TabBar.Style.TEXT, name -> {
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

    private static JScrollPane scrolling(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    /** CardLayout stretches each card to fill the space, so each section sits at the top of its own panel. */
    private static JPanel topAligned(JPanel section) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(section, BorderLayout.NORTH);
        return wrapper;
    }
}
