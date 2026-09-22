package com.lumbridgeguide.ui;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Small factory for themed Swing components. Every component gets an explicit
 * font because RuneLite's global pixel font would otherwise leak in.
 */
final class Ui {

    static final int CONTENT_WIDTH = 188;

    static JLabel label(String text, float size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(LumbridgeGuideTheme.font(style, size));
        label.setForeground(color);
        return label;
    }

    /**
     * Text that wraps at the given pixel width.
     */
    static WrapText wrapped(String text, int width, float size, int style, Color color) {
        WrapText wrapText = new WrapText(width);
        wrapText.setFont(LumbridgeGuideTheme.font(style, size));
        wrapText.setForeground(color);
        wrapText.setText(text);
        return wrapText;
    }

    static JButton button(String text, Color background, Color foreground, Color hover) {
        JButton button = new JButton(text);
        button.setFont(LumbridgeGuideTheme.font(Font.BOLD, 12f));
        button.setForeground(foreground);
        button.setBackground(background);
        button.setOpaque(true);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(7, 12, 7, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent mouseEvent) {
                if (button.isEnabled()) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent mouseEvent) {
                button.setBackground(background);
            }
        });
        return button;
    }

    static JButton primaryButton(String text) {
        return button(text, LumbridgeGuideTheme.ACCENT, LumbridgeGuideTheme.TEXT_INVERSE,
                LumbridgeGuideTheme.ACCENT_HOVER);
    }

    static JButton secondaryButton(String text) {
        return button(text, LumbridgeGuideTheme.SURFACE_OVERLAY, LumbridgeGuideTheme.TEXT_PRIMARY,
                LumbridgeGuideTheme.BORDER);
    }

    /**
     * Word wrapped text measured with the real font metrics. Swing's HTML labels
     * measure and paint differently, which clipped lines, so this draws its own.
     */
    static class WrapText extends JComponent {

        private final int wrapWidth;
        private List<String> lines = new ArrayList<>();
        private String text = "";

        WrapText(int wrapWidth) {
            this.wrapWidth = wrapWidth;
            setAlignmentX(LEFT_ALIGNMENT);
        }

        void setText(String newText) {
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

    /**
     * A small rounded pill with centred text, used for tile types and team names.
     */
    static class Badge extends JComponent {

        private final String text;
        private final Color background;
        private final Color foreground;
        private final Font font = LumbridgeGuideTheme.font(Font.BOLD, 10f);

        Badge(String text, Color background, Color foreground) {
            this.text = text;
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics metrics = getFontMetrics(font);
            return new Dimension(metrics.stringWidth(text) + 14, metrics.getHeight() + 4);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            canvas.setColor(background);
            canvas.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            canvas.setFont(font);
            canvas.setColor(foreground);
            FontMetrics metrics = canvas.getFontMetrics();
            int textX = (getWidth() - metrics.stringWidth(text)) / 2;
            int textY = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            canvas.drawString(text, textX, textY);
            canvas.dispose();
        }
    }

    /**
     * A check mark drawn as lines, because the panel fonts have no glyph for one.
     */
    static class CheckIcon implements Icon {

        private static final int SIZE = 14;

        private final Color color;

        CheckIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(java.awt.Component component, Graphics graphics, int x, int y) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(color);
            canvas.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND,
                    java.awt.BasicStroke.JOIN_ROUND));
            canvas.drawLine(x + 2, y + 7, x + 6, y + 11);
            canvas.drawLine(x + 6, y + 11, x + 12, y + 3);
            canvas.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    private Ui() {
    }
}
