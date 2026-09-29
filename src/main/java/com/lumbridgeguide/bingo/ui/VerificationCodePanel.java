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
 * Shows a bingo verification code on one line in large mono type, with a hint on the right. Clicking anywhere on it
 * copies the code.
 */
class VerificationCodePanel extends JPanel {

    private static final int HEIGHT = 24;
    private static final String HINT = "Click to copy";

    private final Font captionFont = Theme.monoFont(Font.PLAIN, 9f);
    private final Font codeFont = Theme.monoFont(Font.BOLD, 17f);
    private final Font hintFont = Components.font(10f, Font.PLAIN);

    private String code = "";
    private String hint = HINT;

    VerificationCodePanel() {
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                copy();
            }
        });
    }

    void setCode(String newCode) {
        code = newCode == null ? "" : newCode;
        hint = HINT;
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
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        FontMetrics codeMetrics = canvas.getFontMetrics(codeFont);
        int baseline = (HEIGHT - codeMetrics.getHeight()) / 2 + codeMetrics.getAscent();

        canvas.setFont(captionFont);
        canvas.setColor(Theme.TEXT_MUTED);
        canvas.drawString("CODE", 0, baseline);
        int codeX = canvas.getFontMetrics().stringWidth("CODE") + 6;

        canvas.setFont(codeFont);
        canvas.setColor(Theme.ACCENT);
        canvas.drawString(code, codeX, baseline);

        canvas.setFont(hintFont);
        canvas.setColor(Theme.TEXT_MUTED);
        canvas.drawString(hint, getWidth() - canvas.getFontMetrics().stringWidth(hint), baseline);
        canvas.dispose();
    }

    private void copy() {
        if (code.isEmpty()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        hint = "Copied";
        repaint();
    }
}
