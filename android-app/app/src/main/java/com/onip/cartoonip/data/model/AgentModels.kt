package com.onip.cartoonip.data.model

enum class AgentRole {
    ADMIN,
    AGENT,
}

data class AgentDto(
    val id: String,
    val username: String,
    val fullName: String,
    val role: AgentRole,
    val active: Boolean,
    val createdAt: String,
    val photoDataUrl: String? = null,
)

data class CreateAgentRequest(
    val username: String,
    val password: String,
    val fullName: String,
    val role: AgentRole,
)

data class SetActiveRequest(val active: Boolean)

data class ChangeRoleRequest(val role: AgentRole)

data class ResetPasswordRequest(val newPassword: String)
