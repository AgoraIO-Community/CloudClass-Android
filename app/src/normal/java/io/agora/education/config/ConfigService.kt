package io.agora.education.config

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ConfigService {
    @GET("edu/v2/users/{userUuid}/token")
    fun config(@Path("userUuid") userUuid: String): Call<ConfigResponse>
}