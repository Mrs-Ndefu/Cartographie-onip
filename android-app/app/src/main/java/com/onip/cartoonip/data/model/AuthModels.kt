package com.onip.cartoonip.data.model

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val expiresInMinutes: Long,
    val agent: AgentDto,
)
