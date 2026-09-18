package com.onip.cartoonip.data.network

import com.onip.cartoonip.data.model.AgentDto
import com.onip.cartoonip.data.model.ChangeRoleRequest
import com.onip.cartoonip.data.model.CreateAgentRequest
import com.onip.cartoonip.data.model.DashboardStatsDto
import com.onip.cartoonip.data.model.HouseholdDto
import com.onip.cartoonip.data.model.LoginRequest
import com.onip.cartoonip.data.model.LoginResponse
import com.onip.cartoonip.data.model.PageResponse
import com.onip.cartoonip.data.model.ResetPasswordRequest
import com.onip.cartoonip.data.model.SetActiveRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

interface HouseholdApi {
    @GET("api/households")
    suspend fun list(@Query("page") page: Int, @Query("size") size: Int = 25): PageResponse<HouseholdDto>

    @GET("api/households/{id}")
    suspend fun get(@Path("id") id: String): HouseholdDto

    @DELETE("api/households/{id}")
    suspend fun delete(@Path("id") id: String)
}

interface AgentApi {
    @GET("api/agents")
    suspend fun list(): List<AgentDto>

    @POST("api/agents")
    suspend fun create(@Body request: CreateAgentRequest): AgentDto

    @PATCH("api/agents/{id}/active")
    suspend fun setActive(@Path("id") id: String, @Body request: SetActiveRequest): AgentDto

    @PATCH("api/agents/{id}/role")
    suspend fun changeRole(@Path("id") id: String, @Body request: ChangeRoleRequest): AgentDto

    @POST("api/agents/{id}/reset-password")
    suspend fun resetPassword(@Path("id") id: String, @Body request: ResetPasswordRequest): AgentDto
}

interface DashboardApi {
    @GET("api/dashboard/stats")
    suspend fun stats(): DashboardStatsDto
}

class ApiServices(retrofit: retrofit2.Retrofit) {
    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val households: HouseholdApi by lazy { retrofit.create(HouseholdApi::class.java) }
    val agents: AgentApi by lazy { retrofit.create(AgentApi::class.java) }
    val dashboard: DashboardApi by lazy { retrofit.create(DashboardApi::class.java) }
}
