package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import net.runelite.client.ui.FontManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.function.IntConsumer;

/**
 * Previous and next arrows for cycling through the player's active boards.
 */
class BoardNavigatorPanel extends JPanel {

    private final JLabel positionLabel;
    private final JButton previousButton;
    private final JButton nextButton;

    BoardNavigatorPanel(IntConsumer onStep) {
        super(new BorderLayout(6, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(0, 0, 8, 0));

        previousButton = arrowButton("‹");
        nextButton = arrowButton("›");
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

    private static JButton arrowButton(String text) {
        JButton button = Components.secondaryButton(text);
        button.setFont(FontManager.getRunescapeFont());
        button.setBorder(new EmptyBorder(2, 12, 4, 12));
        return button;
    }
}
