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

public class WrapText extends JComponent {

    private final int wrapWidth;
    private int laidOutWidth;
    private List<String> lines = new ArrayList<>();
    private boolean centred;
    private String text = "";

    public WrapText(int wrapWidth) {
        this.wrapWidth = wrapWidth;
        setAlignmentX(LEFT_ALIGNMENT);
    }

    public void setCentred(boolean newCentred) {
        centred = newCentred;
        repaint();
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
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (width <= 0 || width == laidOutWidth) {
            return;
        }

        laidOutWidth = width;
        int lineCount = lines.size();
        lines = wrapLines();
        if (lines.size() != lineCount) {
            revalidate();
        }
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
            int lineX = centred ? (getWidth() - metrics.stringWidth(line)) / 2 : 0;
            canvas.drawString(line, lineX, baseline);
            baseline += metrics.getHeight();
        }
        canvas.dispose();
    }

    private List<String> wrapLines() {
        List<String> wrappedLines = new ArrayList<>();
        FontMetrics metrics = getFontMetrics(getFont());
        int width = laidOutWidth > 0 ? laidOutWidth : wrapWidth;

        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }

            String candidate = current.length() == 0 ? word : current + " " + word;
            if (current.length() > 0 && metrics.stringWidth(candidate) > width) {
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
