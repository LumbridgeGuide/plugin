package com.lumbridgeguide.bingo;

import com.lumbridgeguide.ui.Theme;
import lombok.experimental.UtilityClass;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@UtilityClass
public class ProofImage {

    private static final DateTimeFormatter STAMP_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private static final Color BACKGROUND = new Color(0, 0, 0, 190);

    public static BufferedImage stamp(Image frame, String code) {
        BufferedImage image = new BufferedImage(frame.getWidth(null), frame.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D canvas = image.createGraphics();
        canvas.drawImage(frame, 0, 0, null);
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        canvas.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));

        String text = code + "  " + ZonedDateTime.now().format(STAMP_TIME);
        FontMetrics metrics = canvas.getFontMetrics();
        int width = metrics.stringWidth(text) + 20;
        int height = metrics.getHeight() + 10;
        int x = image.getWidth() - width - 10;
        int y = image.getHeight() - height - 10;
        canvas.setColor(BACKGROUND);
        canvas.fillRect(x, y, width, height);
        canvas.setColor(Theme.accent());
        canvas.drawString(text, x + 10, y + 5 + metrics.getAscent());
        canvas.dispose();

        return image;
    }

    public static void drawXpCard(BufferedImage image, XpReading xp, long target, BufferedImage icon) {
        NumberFormat numbers = NumberFormat.getIntegerInstance(Locale.UK);
        String title = xp.getSkill().getName();
        String gained = xp.getGained() == null
                ? "XP gain not tracked"
                : "+" + numbers.format(xp.getGained()) + " XP since the event started";
        String totals = (target > 0 ? "Target " + numbers.format(target) + "  ·  " : "")
                + "Total " + numbers.format(xp.getTotal()) + " XP";

        Graphics2D canvas = image.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font titleFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
        Font bodyFont = new Font(Font.SANS_SERIF, Font.PLAIN, 16);
        FontMetrics titleMetrics = canvas.getFontMetrics(titleFont);
        FontMetrics bodyMetrics = canvas.getFontMetrics(bodyFont);
        int iconSize = icon == null ? 0 : titleMetrics.getAscent() + 4;
        int padding = 10;
        int width = padding * 2 + Math.max(
                iconSize + (icon == null ? 0 : 8) + titleMetrics.stringWidth(title),
                Math.max(bodyMetrics.stringWidth(gained), bodyMetrics.stringWidth(totals)));
        int height = padding * 2 + Math.max(iconSize, titleMetrics.getHeight()) + 4 + bodyMetrics.getHeight() * 2;
        int x = 10;
        int y = 10;

        canvas.setColor(BACKGROUND);
        canvas.fillRect(x, y, width, height);
        int lineTop = y + padding;
        int textX = x + padding;
        if (icon != null) {
            canvas.drawImage(icon, textX, lineTop, iconSize, iconSize, null);
            textX += iconSize + 8;
        }

        canvas.setFont(titleFont);
        canvas.setColor(Theme.accent());
        canvas.drawString(title, textX, lineTop + (Math.max(iconSize, titleMetrics.getHeight()) + titleMetrics.getAscent()
                - titleMetrics.getDescent()) / 2);
        canvas.setFont(bodyFont);
        canvas.setColor(Theme.TEXT_PRIMARY);
        int bodyTop = lineTop + Math.max(iconSize, titleMetrics.getHeight()) + 4;
        canvas.drawString(gained, x + padding, bodyTop + bodyMetrics.getAscent());
        canvas.drawString(totals, x + padding, bodyTop + bodyMetrics.getHeight() + bodyMetrics.getAscent());
        canvas.dispose();
    }
}
