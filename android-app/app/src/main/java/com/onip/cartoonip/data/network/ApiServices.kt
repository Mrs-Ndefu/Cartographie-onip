package com.onip.cartoonip.data.network

import com.onip.cartoonip.data.model.HouseholdSyncRequest
import com.onip.cartoonip.data.model.LoginRequest
import com.onip.cartoonip.data.model.LoginResponse
import com.onip.cartoonip.data.model.SyncResponse
import okhttp3.MultipartBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
}

interface HouseholdApi {
    @POST("api/households/sync")
    suspend fun sync(@Body request: HouseholdSyncRequest): SyncResponse

    @Multipart
    @POST("api/households/{id}/photo")
    suspend fun uploadPhoto(@Path("id") id: String, @Part file: MultipartBody.Part)
}

class ApiServices(retrofit: Retrofit) {
    val auth: AuthApi by lazy { retrofit.create(AuthApi::class.java) }
    val households: HouseholdApi by lazy { retrofit.create(HouseholdApi::class.java) }
}
