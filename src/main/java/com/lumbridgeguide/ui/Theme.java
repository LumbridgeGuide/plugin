package com.lumbridgeguide.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.HashSet;
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

    public static final Color ACCENT = new Color(0xE07A3A);
    public static final Color ACCENT_HOVER = new Color(0xC96A2E);

    public static final Color SUCCESS = new Color(0x4A9E6E);
    public static final Color WARNING = new Color(0xD4A32A);
    public static final Color ERROR = new Color(0xC4573A);

    private static final String[] MONO_FAMILIES = {
            "JetBrains Mono", "Cascadia Mono", "Consolas", "Menlo", "Monospaced"
    };

    private static final Set<String> INSTALLED_FAMILIES = new HashSet<>(Arrays.asList(
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));

    private static final String MONO = firstInstalled(MONO_FAMILIES, Font.MONOSPACED);

    public static Font monoFont(int style, float size) {
        return new Font(MONO, style, 1).deriveFont(size);
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
