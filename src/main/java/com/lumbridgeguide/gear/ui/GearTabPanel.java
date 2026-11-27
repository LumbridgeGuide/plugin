package com.lumbridgeguide.gear.ui;

import com.lumbridgeguide.gear.GearConfigExportService;
import com.lumbridgeguide.gear.GearTagService;
import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Section;

import javax.inject.Inject;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

public class GearTabPanel extends JPanel {

    private final BankTabSection bankTabSection;

    @Inject
    public GearTabPanel(GearTagService gearTagService, GearConfigExportService gearConfigExportService) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 0, 0));

        bankTabSection = new BankTabSection(gearTagService);

        Section content = new Section(0);
        content.add(new CreateConfigSection(gearConfigExportService));
        content.add(Box.createVerticalStrut(20));
        content.add(bankTabSection);

        JPanel topAligned = new JPanel(new BorderLayout());
        topAligned.setOpaque(false);
        topAligned.add(content, BorderLayout.NORTH);
        add(Components.scrolling(topAligned), BorderLayout.CENTER);
    }

    public void setBankOpen(boolean open) {
        bankTabSection.setBankOpen(open);
    }
}
