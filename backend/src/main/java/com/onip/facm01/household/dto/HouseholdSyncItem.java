package com.onip.facm01.household.dto;

import com.onip.facm01.household.HouseholdStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HouseholdSyncItem(
        @NotNull UUID id,
        String codeMenage,
        Integer nombreMembres,
        @NotNull PersonDto chef,
        List<PersonDto> membres,
        @NotNull AddressDto address,
        GeoLocationDto location,
        @NotNull FormMetaDto meta,
        @NotNull HouseholdStatus status,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt) {
}
