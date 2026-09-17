package com.onip.facm01.household.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HouseholdSyncRequest(@NotEmpty @Valid List<HouseholdSyncItem> households) {
}
