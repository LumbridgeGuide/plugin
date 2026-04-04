package com.lumbridgeguide.ui;

import java.awt.Color;

public final class LumbridgeGuideTheme {

    public static final Color PANEL_BACKGROUND = new Color(43, 43, 43);
    public static final Color PANEL_BACKGROUND_LIGHTER = new Color(50, 50, 50);
    public static final Color BRAND_PRIMARY = new Color(0, 190, 165);
    public static final Color BRAND_ACCENT = new Color(235, 134, 52);

    public static final Color TEXT_PRIMARY = Color.WHITE;
    public static final Color TEXT_SECONDARY = new Color(176, 176, 176);
    public static final Color TEXT_MUTED = new Color(128, 128, 128);

    public static final Color STATUS_SUCCESS = new Color(0, 190, 0);
    public static final Color STATUS_ERROR = new Color(220, 50, 50);
    public static final Color STATUS_PENDING = new Color(176, 176, 176);

    public static final Color TILE_BACKGROUND = new Color(55, 55, 55);
    public static final Color TILE_HOVER = new Color(65, 65, 65);
    public static final Color TILE_CLAIMED_OVERLAY = new Color(0, 190, 0, 40);
    public static final Color TILE_BORDER = new Color(70, 70, 70);

    public static final Color SEPARATOR = new Color(60, 60, 60);

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

    private LumbridgeGuideTheme() {
    }
}
