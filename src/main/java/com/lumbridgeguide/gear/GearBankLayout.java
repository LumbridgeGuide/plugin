package com.lumbridgeguide.gear;

import com.lumbridgeguide.gear.data.PluginGearData;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@UtilityClass
public class GearBankLayout {

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

    /** Item ids by bank position, row by row, -1 where empty, leaving out unknown slots and overflow */
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

    /** The row is counted within the setup, below the padding row */
    private static void place(int[] layout, int row, int column, int itemId) {
        layout[(row + PADDING_ROWS) * COLUMNS + column] = itemId;
    }
}
