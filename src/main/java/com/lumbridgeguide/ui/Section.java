package com.lumbridgeguide.ui;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Dimension;

/**
 * A vertical group of controls with space above and below and no box around it, so type and spacing separate the
 * parts of a tab.
 */
public class Section extends JPanel {

    public Section(int spacing) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(spacing, 0, spacing, 0));
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /** Sections stretch across a vertical layout but keep their own height. */
    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
