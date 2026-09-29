package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.ui.Theme;
import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.function.IntConsumer;

/** Steps between the viewer's active boards. Hidden when there is only one. */
class BoardNavigatorPanel extends JPanel {

    private final JLabel positionLabel;

    BoardNavigatorPanel(IntConsumer onStep) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 4, 0));

        JButton previousButton = arrowButton("‹");
        JButton nextButton = arrowButton("›");
        previousButton.addActionListener(event -> onStep.accept(-1));
        nextButton.addActionListener(event -> onStep.accept(1));

        positionLabel = new JLabel("", SwingConstants.CENTER);
        positionLabel.setFont(FontManager.getRunescapeFont());
        positionLabel.setForeground(Theme.TEXT_SECONDARY);

        add(previousButton, BorderLayout.WEST);
        add(positionLabel, BorderLayout.CENTER);
        add(nextButton, BorderLayout.EAST);
    }

    void update(int currentIndex, int total) {
        positionLabel.setText("Board " + (currentIndex + 1) + " of " + total);
        setVisible(total > 1);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private static JButton arrowButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeBoldFont());
        button.setForeground(Theme.TEXT_SECONDARY);
        button.setBorder(new EmptyBorder(2, 4, 2, 4));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
