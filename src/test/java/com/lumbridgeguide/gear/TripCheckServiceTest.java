package com.lumbridgeguide.gear;

import com.lumbridgeguide.gear.data.PluginGearData;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TripCheckServiceTest {

    private static final int WHIP = 4151;
    private static final int WHIP_VARIANT = 4178;
    private static final int SHARK = 385;
    private static final int PRAYER_POTION = 2434;
    private static final int DEATH_RUNE = 560;

    @Test
    public void countsVariationsAsTheSameItemAndSaysWhereMissingItemsAre() {
        PluginGearData gear = new PluginGearData("id", "Synthetic trip", List.of(),
                Map.of("weapon", WHIP), Arrays.asList(SHARK, SHARK, PRAYER_POTION, null), List.of(DEATH_RUNE));

        TripCheckService.Check check = TripCheckService.compare(gear,
                List.of(WHIP_VARIANT), List.of(SHARK), List.of(), Set.of(SHARK),
                itemId -> itemId == WHIP_VARIANT ? WHIP : itemId, itemId -> "item " + itemId);

        assertEquals(1, check.getWornHave());
        assertEquals(1, check.getInventoryHave());
        assertEquals(3, check.getInventoryTotal());
        assertEquals(0, check.getPouchHave());
        assertEquals(List.of(
                new TripCheckService.Missing("item " + SHARK, 1, TripCheckService.Whereabouts.IN_BANK),
                new TripCheckService.Missing("item " + PRAYER_POTION, 1, TripCheckService.Whereabouts.NOT_OWNED),
                new TripCheckService.Missing("item " + DEATH_RUNE, 1, TripCheckService.Whereabouts.NOT_OWNED)),
                check.getMissing());
    }
}
