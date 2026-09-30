package com.lumbridgeguide.gear;

import org.junit.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.Assert.assertEquals;

public class GearTagServiceTest {

    private static final int SANFEW_4 = 10925;
    private static final int SANFEW_3 = 10927;
    private static final int DRAGON_DAGGER = 1215;
    private static final int DRAGON_DAGGER_PP = 5698;
    private static final IntUnaryOperator VARIATION =
            itemId -> itemId == SANFEW_3 ? SANFEW_4 : itemId == DRAGON_DAGGER_PP ? DRAGON_DAGGER : itemId;

    @Test
    public void usesTheExactItemWhenTheBankHasIt() {
        Set<Integer> owned = Set.of(DRAGON_DAGGER, DRAGON_DAGGER_PP);
        Map<Integer, Integer> byVariation = Map.of(DRAGON_DAGGER, DRAGON_DAGGER);

        assertEquals(DRAGON_DAGGER_PP, GearTagService.bankItemFor(DRAGON_DAGGER_PP, owned, byVariation, VARIATION));
    }

    @Test
    public void fallsBackToAnotherDoseTheBankHas() {
        assertEquals(SANFEW_3, GearTagService.bankItemFor(SANFEW_4, Set.of(SANFEW_3), Map.of(SANFEW_4, SANFEW_3),
                VARIATION));
    }

    @Test
    public void isMissingWhenNoVariationIsInTheBank() {
        assertEquals(-1, GearTagService.bankItemFor(SANFEW_4, Set.of(), Map.of(), VARIATION));
    }
}
