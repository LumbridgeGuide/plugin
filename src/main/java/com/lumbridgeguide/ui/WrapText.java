package com.lumbridgeguide.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

/**
 * Word wrapped text measured with the real font metrics. Swing's HTML labels
 * measure and paint differently, which clipped lines, so this draws its own.
 */
public class WrapText extends JComponent {

    private final int wrapWidth;
    private List<String> lines = new ArrayList<>();
    private String text = "";

    public WrapText(int wrapWidth) {
        this.wrapWidth = wrapWidth;
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public void setText(String newText) {
        text = newText == null ? "" : newText;
        lines = wrapLines();
        revalidate();
        repaint();
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        lines = wrapLines();
        revalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(wrapWidth, Math.max(1, lines.size()) * getFontMetrics(getFont()).getHeight());
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(getFont());
        canvas.setColor(getForeground());
        FontMetrics metrics = canvas.getFontMetrics();
        int baseline = metrics.getAscent();
        for (String line : lines) {
            canvas.drawString(line, 0, baseline);
            baseline += metrics.getHeight();
        }
        canvas.dispose();
    }

    private List<String> wrapLines() {
        List<String> wrappedLines = new ArrayList<>();
        FontMetrics metrics = getFontMetrics(getFont());
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && metrics.stringWidth(candidate) > wrapWidth) {
                wrappedLines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            wrappedLines.add(current.toString());
        }
        return wrappedLines;
    }
}
