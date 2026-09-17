package com.onip.facm01.agent.dto;

import com.onip.facm01.agent.AgentRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull AgentRole role) {
}
