package com.lumbridgeguide.ui;

import lombok.experimental.UtilityClass;

import java.awt.Color;

@UtilityClass
public class Theme {

    public static final Color PANEL_BACKGROUND = new Color(0x1A1A1E);
    public static final Color SURFACE_RAISED = new Color(0x242428);
    public static final Color SURFACE_OVERLAY = new Color(0x2E2E33);
    public static final Color SURFACE_INSET = new Color(0x16161A);

    public static final Color BORDER = new Color(0x3A3A40);
    public static final Color BORDER_SUBTLE = new Color(0x2E2E33);

    public static final Color TEXT_PRIMARY = new Color(0xE4E4E7);
    public static final Color TEXT_SECONDARY = new Color(0xA1A1A6);
    public static final Color TEXT_MUTED = new Color(0x97979E);
    public static final Color TEXT_INVERSE = new Color(0x1A1A1E);

    public static final Color DEFAULT_ACCENT = new Color(0xE07A3A);

    private static Color accent = DEFAULT_ACCENT;
    private static Color accentHover = hoverOf(DEFAULT_ACCENT);
    private static Color accentText = textOf(DEFAULT_ACCENT);

    public static final Color SUCCESS = new Color(0x5AA77B);
    public static final Color WARNING = new Color(0xD4A32A);
    public static final Color ERROR = new Color(0xD3836D);

    public static final int ARC = 4;

    public static Color accent() {
        return accent;
    }

    public static Color accentHover() {
        return accentHover;
    }

    public static void setAccent(Color newAccent) {
        accent = newAccent == null ? DEFAULT_ACCENT : newAccent;
        accentHover = hoverOf(accent);
        accentText = textOf(accent);
    }

    public static Color accentText() {
        return accentText;
    }

    public static Color accentWash() {
        return wash(accent);
    }

    public static Color wash(Color colour) {
        return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 38);
    }

    private static Color textOf(Color colour) {
        return new Color(colour.getRed() + Math.round((255 - colour.getRed()) * 0.18f),
                colour.getGreen() + Math.round((255 - colour.getGreen()) * 0.18f),
                colour.getBlue() + Math.round((255 - colour.getBlue()) * 0.18f));
    }

    private static Color hoverOf(Color colour) {
        return new Color(Math.round(colour.getRed() * 0.88f), Math.round(colour.getGreen() * 0.88f),
                Math.round(colour.getBlue() * 0.88f));
    }

    public static Color parseTeamColor(String hex) {
        if (hex == null || hex.isEmpty()) {
            return TEXT_MUTED;
        }
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (NumberFormatException ignored) {
            return TEXT_MUTED;
        }
    }
}
