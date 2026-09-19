package com.onip.cartoonip.data.model

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(
    val token: String,
    val expiresInMinutes: Long,
    val agent: AgentDto,
)

data class AgentDto(
    val id: String,
    val username: String,
    val fullName: String,
    val role: String,
    val active: Boolean,
    val photoDataUrl: String? = null,
)

data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
