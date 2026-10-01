package com.onip.facm01.dashboard;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DemographicsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Test
    void ageIsComputedFromTheAppsDateFormat() {
        assertEquals(30, DemographicsService.age("12/05/1996", TODAY));
        assertEquals(17, DemographicsService.age("02/10/2008", TODAY)); // 18 ans demain : encore mineur
        assertEquals(18, DemographicsService.age("01/10/2008", TODAY)); // 18 ans aujourd'hui : majeur
    }

    @Test
    void ageAcceptsVariantFormats() {
        assertEquals(30, DemographicsService.age("2/5/1996", TODAY));
        assertEquals(30, DemographicsService.age("1996-05-12", TODAY));
    }

    @Test
    void unknownOrImplausibleDatesGiveNoAge() {
        assertNull(DemographicsService.age(null, TODAY));
        assertNull(DemographicsService.age("", TODAY));
        assertNull(DemographicsService.age("pas une date", TODAY));
        assertNull(DemographicsService.age("01/01/2030", TODAY)); // dans le futur
        assertNull(DemographicsService.age("01/01/1850", TODAY)); // plus de 120 ans
    }
}
