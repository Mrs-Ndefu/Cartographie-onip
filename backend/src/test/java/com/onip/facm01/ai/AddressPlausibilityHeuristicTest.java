package com.onip.facm01.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressPlausibilityHeuristicTest {

    @Test
    void flagsRepeatedCharactersAndKeyboardMash() {
        assertTrue(AddressPlausibilityHeuristic.isImplausible("aaaaa", "AVENUE DE LA PAIX", null));
        assertTrue(AddressPlausibilityHeuristic.isImplausible("MATONGE", "qsdqsd", null));
        assertTrue(AddressPlausibilityHeuristic.isImplausible("MATONGE", "azerty", null));
        assertTrue(AddressPlausibilityHeuristic.isImplausible("abababab", "AVENUE DE LA PAIX", null));
    }

    @Test
    void flagsPlaceholderWordsAndConsonantOnlyText() {
        assertTrue(AddressPlausibilityHeuristic.isImplausible("test", "AVENUE DE LA PAIX", null));
        assertTrue(AddressPlausibilityHeuristic.isImplausible("MATONGE", "xzqplm", null));
    }

    @Test
    void leavesPlausibleCongoleseAddressesUntouched() {
        assertFalse(AddressPlausibilityHeuristic.isImplausible("MATONGE", "AVENUE DE LA PAIX", "12"));
        assertFalse(AddressPlausibilityHeuristic.isImplausible("NGABA", "AVENUE KIMWENZA", "IMM. BAOBAB"));
        assertFalse(AddressPlausibilityHeuristic.isImplausible("BANDALUNGWA", "AVENUE DES POIDS LOURDS", null));
    }

    @Test
    void leavesBlankOrVeryShortFieldsUntouched() {
        // Un champ vide ou trop court n'est pas forcément du charabia : ce n'est pas le rôle de
        // cette heuristique de forcer un champ à être rempli.
        assertFalse(AddressPlausibilityHeuristic.isImplausible(null, null, null));
        assertFalse(AddressPlausibilityHeuristic.isImplausible("", "", ""));
        assertFalse(AddressPlausibilityHeuristic.isImplausible("A", "B2", null));
    }

    @Test
    void ignoresTheNumeroFieldEntirely() {
        // Le numéro n'est pas passé à isImplausible (cf. AddressAiCheckService) : court et
        // numérique par nature, les règles ci-dessus n'ont pas de sens pour lui.
        assertFalse(AddressPlausibilityHeuristic.isImplausible("MATONGE", "AVENUE DE LA PAIX", "123"));
    }
}
