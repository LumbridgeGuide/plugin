package com.lumbridgeguide.gear;

import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntUnaryOperator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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

    @Test
    public void namesTagsOutsideTheUsersOwnTags() {
        assertEquals("lg-zulrah-mage", GearTagService.tagName("Zulrah mage"));
        assertEquals("lg-gear", GearTagService.tagName("  "));
    }

    @Test
    public void extractsTheCodeFromALinkWithSurroundingText() {
        assertEquals(Optional.of("0f8e7d6c-1a2b-3c4d-5e6f-a1b2c3d4e5f6"), GearTagService.extractCode(
                "see https://lumbridge.guide/gear/0F8E7D6C-1A2B-3C4D-5E6F-A1B2C3D4E5F6 for it"));
    }

    @Test
    public void extractsNothingFromGarbage() {
        assertFalse(GearTagService.extractCode("not a gear code").isPresent());
    }
}
