package com.onip.facm01.site.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SiteSyncRequest(@NotEmpty @Valid List<SiteSyncItem> sites) {
}
