package com.lumbridgeguide.ui;

import java.awt.Color;

/**
 * Colours copied from the Lumbridge Guide website's dark theme so the
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

    private Theme() {
    }
}
