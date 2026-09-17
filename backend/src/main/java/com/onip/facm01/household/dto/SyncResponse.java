package com.onip.facm01.household.dto;

import java.util.List;
import java.util.UUID;

public record SyncResponse(List<UUID> accepted, List<SyncRejection> rejected) {
}
