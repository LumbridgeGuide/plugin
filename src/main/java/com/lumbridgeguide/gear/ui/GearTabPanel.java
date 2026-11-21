package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.ui.TabBar;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.Arrays;

public class GearTabPanel extends JPanel {

    private static final String BANK_TAB = "Bank tab";
    private static final String CREATE_CONFIG = "Create";

    private final BankTabSection bankTabSection;

    public GearTabPanel(GearTagService gearTagService, GearConfigExportService gearConfigExportService,
                        boolean includeMissingByDefault) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        bankTabSection = new BankTabSection(gearTagService, includeMissingByDefault);
        CardLayout cards = new CardLayout();
        JPanel display = new JPanel(cards);
        display.setOpaque(false);
        display.add(topAligned(bankTabSection), BANK_TAB);
        display.add(topAligned(new CreateConfigSection(gearConfigExportService)), CREATE_CONFIG);

        TabBar subtabs = new TabBar(Arrays.asList(BANK_TAB, CREATE_CONFIG), TabBar.Style.PILL,
                name -> cards.show(display, name));
        subtabs.setBorder(new EmptyBorder(0, 0, 10, 0));

        add(subtabs, BorderLayout.NORTH);
        add(display, BorderLayout.CENTER);
    }

    public void setBankOpen(boolean open) {
        bankTabSection.setBankOpen(open);
    }

    private static JPanel topAligned(JPanel section) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(section, BorderLayout.NORTH);
        return wrapper;
    }
}
