package com.onip.facm01.auth.dto;

import com.onip.facm01.agent.dto.AgentDto;

public record LoginResponse(String token, long expiresInMinutes, AgentDto agent) {
}
