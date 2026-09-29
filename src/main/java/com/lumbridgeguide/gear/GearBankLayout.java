package com.lumbridgeguide.gear;

import com.lumbridgeguide.gear.data.PluginGearData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Lays a gear config out in an 8-column bank tab in its in-game shape: the equipment slots on the left (columns 1 to
 * 3) as they sit on the equipment screen, the rune pouch runes under them, and the 28 inventory slots as a 4 by 7 grid
 * on the right (columns 5 to 8). Column 4 stays empty to separate the two, and an empty row above and below pads the
 * setup. The 8 columns are all in use, so there is no room to pad the sides.
 */
public final class GearBankLayout {

    public static final int COLUMNS = 8;
    public static final int PADDING_ROWS = 1;
    private static final int CONTENT_ROWS = 7;
    public static final int ROWS = CONTENT_ROWS + 2 * PADDING_ROWS;

    private static final int INVENTORY_COLUMN = 4;
    private static final int INVENTORY_WIDTH = 4;
    private static final int RUNE_POUCH_ROW = 6;

    private static final Map<String, int[]> EQUIPMENT_CELLS = Map.ofEntries(
            Map.entry("head", new int[] {0, 1}),
            Map.entry("ammo2", new int[] {0, 2}),
            Map.entry("cape", new int[] {1, 0}),
            Map.entry("neck", new int[] {1, 1}),
            Map.entry("ammo", new int[] {1, 2}),
            Map.entry("weapon", new int[] {2, 0}),
            Map.entry("body", new int[] {2, 1}),
            Map.entry("shield", new int[] {2, 2}),
            Map.entry("legs", new int[] {3, 1}),
            Map.entry("hands", new int[] {4, 0}),
            Map.entry("feet", new int[] {4, 1}),
            Map.entry("ring", new int[] {4, 2}));

    private GearBankLayout() {
    }

    /**
     * The item id at each bank position, row by row, with -1 where the position is empty. Unknown equipment slots and
     * anything past the grid are left out.
     */
    public static int[] positions(PluginGearData gear) {
        int[] layout = new int[COLUMNS * ROWS];
        Arrays.fill(layout, -1);

        if (gear.getEquipment() != null) {
            gear.getEquipment().forEach((slot, itemId) -> {
                int[] cell = EQUIPMENT_CELLS.get(slot);
                if (cell != null && itemId != null) {
                    place(layout, cell[0], cell[1], itemId);
                }
            });
        }

        List<Integer> inventory = gear.getInventory();
        if (inventory != null) {
            for (int slot = 0; slot < inventory.size() && slot < INVENTORY_WIDTH * CONTENT_ROWS; slot++) {
                Integer itemId = inventory.get(slot);
                if (itemId != null) {
                    place(layout, slot / INVENTORY_WIDTH, INVENTORY_COLUMN + slot % INVENTORY_WIDTH, itemId);
                }
            }
        }

        List<Integer> runePouch = gear.getRunePouch();
        if (runePouch != null) {
            for (int slot = 0; slot < runePouch.size() && slot < INVENTORY_COLUMN; slot++) {
                Integer itemId = runePouch.get(slot);
                if (itemId != null) {
                    place(layout, RUNE_POUCH_ROW, slot, itemId);
                }
            }
        }

        return layout;
    }

    /** Rows are counted within the setup; the padding row above it is added here. */
    private static void place(int[] layout, int row, int column, int itemId) {
        layout[(row + PADDING_ROWS) * COLUMNS + column] = itemId;
    }
}
