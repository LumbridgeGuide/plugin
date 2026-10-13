package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;
import net.runelite.client.ui.FontManager;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.IntConsumer;

/** Previous and next controls for moving between active boards */
class BoardNavigatorPanel extends JPanel {

    private final JLabel positionLabel;

    BoardNavigatorPanel(IntConsumer onStep) {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        JButton previousButton = arrowButton("‹");
        JButton nextButton = arrowButton("›");
        previousButton.setToolTipText("Previous board");
        nextButton.setToolTipText("Next board");
        previousButton.addActionListener(event -> onStep.accept(-1));
        nextButton.addActionListener(event -> onStep.accept(1));

        positionLabel = Components.monoLabel("", 10f, Font.PLAIN, Theme.TEXT_MUTED);
        positionLabel.setBorder(new EmptyBorder(0, 2, 0, 2));

        add(previousButton);
        add(positionLabel);
        add(nextButton);
    }

    void update(int currentIndex, int total) {
        positionLabel.setText((currentIndex + 1) + "/" + total);
        setVisible(total > 1);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    private static JButton arrowButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeBoldFont());
        button.setForeground(Theme.TEXT_SECONDARY);
        button.setBorder(new EmptyBorder(0, 2, 0, 2));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
}
