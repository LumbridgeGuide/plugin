package com.lumbridgeguide.gear;

import com.lumbridgeguide.gear.data.PluginGearData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class GearBankLayoutTest {

    /** Rows are counted within the setup, below the padding row. */
    private static int at(int[] layout, int row, int column) {
        return layout[(row + GearBankLayout.PADDING_ROWS) * GearBankLayout.COLUMNS + column];
    }

    @Test
    public void padsTheSetupWithAnEmptyRowAboveAndBelow() {
        List<Integer> inventory = new ArrayList<>(Collections.nCopies(28, 385));
        PluginGearData gear = new PluginGearData("id", "Full", List.of(), Map.of("head", 1), inventory, null);

        int[] layout = GearBankLayout.positions(gear);
        int lastRow = GearBankLayout.ROWS - 1;

        for (int column = 0; column < GearBankLayout.COLUMNS; column++) {
            assertEquals(-1, layout[column]);
            assertEquals(-1, layout[lastRow * GearBankLayout.COLUMNS + column]);
        }
        assertEquals(385, at(layout, 6, 7));
    }

    @Test
    public void placesEquipmentInventoryAndRunePouchSideBySide() {
        List<Integer> inventory = new ArrayList<>(Collections.nCopies(28, null));
        inventory.set(0, 2434);
        inventory.set(5, 391);
        inventory.set(27, 12791);
        PluginGearData gear = new PluginGearData("id", "Vorkath ranged", List.of(),
                Map.of("head", 12931, "weapon", 21012, "ring", 19710, "ammo2", 22227),
                inventory, Arrays.asList(560, null, 563));

        int[] layout = GearBankLayout.positions(gear);

        assertEquals(12931, at(layout, 0, 1));
        assertEquals(22227, at(layout, 0, 2));
        assertEquals(21012, at(layout, 2, 0));
        assertEquals(19710, at(layout, 4, 2));
        assertEquals(2434, at(layout, 0, 4));
        assertEquals(391, at(layout, 1, 5));
        assertEquals(12791, at(layout, 6, 7));
        assertEquals(560, at(layout, 6, 0));
        assertEquals(-1, at(layout, 6, 1));
        assertEquals(563, at(layout, 6, 2));
        assertEquals(-1, at(layout, 0, 3));
    }

    @Test
    public void ignoresUnknownSlotsAndMissingSections() {
        PluginGearData gear = new PluginGearData("id", "Empty", List.of(), Map.of("tail", 1), null, null);

        int[] layout = GearBankLayout.positions(gear);

        assertEquals(GearBankLayout.COLUMNS * GearBankLayout.ROWS, layout.length);
        assertEquals(0, Arrays.stream(layout).filter(itemId -> itemId != -1).count());
    }

    @Test
    public void summarySaysWhatHappenedToMissingItems() {
        assertEquals("Tagged 5 items as \"vorkath\".", GearTagService.summary("vorkath", 5, 0, true));
        assertEquals("Tagged 5 items as \"vorkath\". 2 items not in your bank, shown as placeholders.",
                GearTagService.summary("vorkath", 5, 2, true));
        assertEquals("Tagged 3 items as \"vorkath\". 1 item not in your bank was left out.",
                GearTagService.summary("vorkath", 3, 1, false));
    }
}
