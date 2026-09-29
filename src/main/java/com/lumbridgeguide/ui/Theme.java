package com.lumbridgeguide.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Colours and fonts copied from the Lumbridge Guide website's dark theme so the
 * plugin panel looks like part of the same product.
 */
public final class Theme {

    public static final Color PANEL_BACKGROUND = new Color(0x1A1A1E);
    public static final Color SURFACE_RAISED = new Color(0x242428);
    public static final Color SURFACE_OVERLAY = new Color(0x2E2E33);
    public static final Color SURFACE_INSET = new Color(0x16161A);

    public static final Color BORDER = new Color(0x3A3A40);
    public static final Color BORDER_SUBTLE = new Color(0x2E2E33);

    public static final Color TEXT_PRIMARY = new Color(0xE4E4E7);
    public static final Color TEXT_SECONDARY = new Color(0xA1A1A6);
    public static final Color TEXT_MUTED = new Color(0x6E6E76);
    public static final Color TEXT_INVERSE = new Color(0x1A1A1E);

    public static final Color DEFAULT_ACCENT = new Color(0xE07A3A);

    private static Color accent = DEFAULT_ACCENT;
    private static Color accentHover = hoverOf(DEFAULT_ACCENT);

    public static final Color SUCCESS = new Color(0x4A9E6E);
    public static final Color WARNING = new Color(0xD4A32A);
    public static final Color ERROR = new Color(0xC4573A);

    /** Corner diameter for cards, buttons, inputs and chips: a slight softening of square corners. */
    public static final int ARC = 4;

    private static final String[] MONO_FAMILIES = {
            "JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo", "Monospaced"
    };

    private static final Set<String> INSTALLED_FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static final String MONO = firstInstalled(MONO_FAMILIES, Font.MONOSPACED);

    /** The highlight colour for selected tabs, primary buttons and codes, set from the plugin's settings. */
    public static Color accent() {
        return accent;
    }

    public static Color accentHover() {
        return accentHover;
    }

    public static void setAccent(Color newAccent) {
        accent = newAccent == null ? DEFAULT_ACCENT : newAccent;
        accentHover = hoverOf(accent);
    }

    private static Color hoverOf(Color colour) {
        return new Color(Math.round(colour.getRed() * 0.88f), Math.round(colour.getGreen() * 0.88f),
                Math.round(colour.getBlue() * 0.88f));
    }

    public static Font monoFont(int style, float size) {
        return new Font(MONO, style, 1).deriveFont(size);
    }

    public static final float LABEL_SIZE = 9f;
    public static final float LABEL_TRACKING = 0.08f;

    /** The small spaced-out uppercase mono type used for section labels, such as "TILES". */
    public static Font labelFont() {
        return monoFont(Font.PLAIN, LABEL_SIZE).deriveFont(Map.of(TextAttribute.TRACKING, LABEL_TRACKING));
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

    private static String firstInstalled(String[] candidates, String fallback) {
        for (String candidate : candidates) {
            if (INSTALLED_FAMILIES.contains(candidate)) {
                return candidate;
            }
        }
        return fallback;
    }

    private Theme() {
    }
}
