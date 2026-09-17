package com.onip.facm01.household.dto;

import java.util.UUID;

public record SyncRejection(UUID id, String reason) {
}
