package com.pjminecraft.luodiguigen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SaplingKindTest {
    @Test
    void oakStaysOakWhenItsOwnLeavesDominate() {
        assertEquals(SaplingKind.OAK, SaplingKind.choose(WoodType.OAK, 10, 2, 1));
    }

    @Test
    void azaleaWinsWhenThoseLeavesDominate() {
        assertEquals(SaplingKind.AZALEA, SaplingKind.choose(WoodType.OAK, 1, 6, 2));
        assertEquals(SaplingKind.FLOWERING_AZALEA, SaplingKind.choose(WoodType.OAK, 0, 3, 3));
    }

    @Test
    void otherWoodsIgnoreAzaleaCounts() {
        assertEquals(SaplingKind.SPRUCE, SaplingKind.choose(WoodType.SPRUCE, 0, 9, 9));
        assertEquals(SaplingKind.POPLAR, SaplingKind.of(WoodType.POPLAR));
        assertEquals(SaplingKind.MANGROVE, SaplingKind.of(WoodType.MANGROVE));
    }
}
