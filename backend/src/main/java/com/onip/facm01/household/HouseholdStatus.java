package com.onip.facm01.household;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum HouseholdStatus {
    BROUILLON("brouillon"),
    COMPLET("complet"),
    A_VERIFIER("a_verifier");

    private final String value;

    HouseholdStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static HouseholdStatus fromValue(String value) {
        for (HouseholdStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut de ménage inconnu : " + value);
    }
}
