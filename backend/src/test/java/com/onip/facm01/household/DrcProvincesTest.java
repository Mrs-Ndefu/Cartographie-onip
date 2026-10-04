package com.onip.facm01.household;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrcProvincesTest {

    @Test
    void kinshasaIsAThenAlphabeticalOrder() {
        assertEquals(Optional.of("A"), DrcProvinces.letterOf("KINSHASA"));
        assertEquals(Optional.of("B"), DrcProvinces.letterOf("BAS-UELE"));
        assertEquals(Optional.of("C"), DrcProvinces.letterOf("ÉQUATEUR"));
        assertEquals(Optional.of("Z"), DrcProvinces.letterOf("TSHUAPA"));
        assertEquals(26, DrcProvinces.PROVINCES_BY_LETTER.size());
    }

    @Test
    void letterIgnoresCaseAndAccents() {
        assertEquals(Optional.of("A"), DrcProvinces.letterOf("Kinshasa"));
        assertEquals(Optional.of("C"), DrcProvinces.letterOf("equateur"));
    }

    @Test
    void unknownProvinceHasNoLetter() {
        assertEquals(Optional.empty(), DrcProvinces.letterOf("PAS UNE PROVINCE"));
        assertEquals(Optional.empty(), DrcProvinces.letterOf(""));
    }
}
