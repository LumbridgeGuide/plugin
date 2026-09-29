package com.lumbridgeguide.bingo.ui;

import com.lumbridgeguide.ui.Components;
import com.lumbridgeguide.ui.Theme;

import javax.swing.JPanel;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Shows a bingo verification code as large characters in individual boxes, like the website. Clicking anywhere on it
 * copies the code.
 */
class VerificationCodePanel extends JPanel {

    private static final int GAP = 3;
    private static final int HEIGHT = 32;
    private static final String HINT = "Click to copy";

    private final Font codeFont = Theme.monoFont(Font.BOLD, 15f);

    private String code = "";

    VerificationCodePanel() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(HINT);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                copy();
            }
        });
    }

    void setCode(String newCode) {
        code = newCode == null ? "" : newCode;
        setToolTipText(HINT);
        setVisible(!code.isEmpty());
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Components.CONTENT_WIDTH, HEIGHT);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (code.isEmpty()) {
            return;
        }

        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(codeFont);
        FontMetrics metrics = canvas.getFontMetrics();

        int count = code.length();
        int boxWidth = (getWidth() - GAP * (count - 1)) / count;
        for (int index = 0; index < count; index++) {
            int boxX = index * (boxWidth + GAP);
            canvas.setColor(Theme.SURFACE_INSET);
            canvas.fillRoundRect(boxX, 0, boxWidth, HEIGHT - 1, Theme.ARC, Theme.ARC);
            canvas.setColor(Theme.BORDER);
            canvas.drawRoundRect(boxX, 0, boxWidth - 1, HEIGHT - 2, Theme.ARC, Theme.ARC);

            String character = String.valueOf(code.charAt(index));
            int textX = boxX + (boxWidth - metrics.stringWidth(character)) / 2;
            int textY = (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
            canvas.setColor(Theme.accent());
            canvas.drawString(character, textX, textY);
        }
        canvas.dispose();
    }

    private void copy() {
        if (code.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        setToolTipText("Copied!");
    }
}
