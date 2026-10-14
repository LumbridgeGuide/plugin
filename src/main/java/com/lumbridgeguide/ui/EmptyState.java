package com.lumbridgeguide.ui;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A centred block for when there is nothing to show yet: an empty bingo grid, a title, a line of help and actions. */
public class EmptyState extends Section {

    public EmptyState(String title, String message, JButton... actions) {
        super(16);

        EmptyGrid grid = new EmptyGrid();
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = Components.label(title, 14f, Font.BOLD, Theme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        WrapText messageText = Components.wrapped(message, Components.CONTENT_WIDTH - 40, 12f, Font.PLAIN,
                Theme.TEXT_SECONDARY);
        messageText.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageText.setCentred(true);

        add(grid);
        add(Box.createVerticalStrut(10));
        add(titleLabel);
        add(Box.createVerticalStrut(4));
        add(messageText);
        if (actions.length > 0) {
            JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            actionRow.setOpaque(false);
            actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);
            for (JButton action : actions) {
                actionRow.add(action);
            }
            actionRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, actionRow.getPreferredSize().height));
            add(Box.createVerticalStrut(12));
            add(actionRow);
        }
    }

    /** A 3 by 3 grid of empty squares, the board before any tiles exist. */
    private static final class EmptyGrid extends JComponent {

        private static final int CELL = 16;
        private static final int GAP = 3;
        private static final int SIZE = CELL * 3 + GAP * 2;

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(SIZE, SIZE);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    int x = (getWidth() - SIZE) / 2 + column * (CELL + GAP);
                    int y = row * (CELL + GAP);
                    canvas.setColor(Theme.SURFACE_INSET);
                    canvas.fillRoundRect(x, y, CELL, CELL, Theme.ARC, Theme.ARC);
                    canvas.setColor(Theme.BORDER);
                    canvas.drawRoundRect(x, y, CELL - 1, CELL - 1, Theme.ARC, Theme.ARC);
                }
            }
            canvas.dispose();
        }
    }
}
