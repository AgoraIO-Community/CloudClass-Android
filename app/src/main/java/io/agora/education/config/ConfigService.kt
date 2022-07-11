package io.agora.education.config

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ConfigService {
    @GET("edu/v2/users/{userUuid}/token")
    fun config(@Path("userUuid") userUuid: String): Call<ConfigResponse>

    /**
     * token007
     */
    @GET("edu/v3/rooms/{roomUuid}/roles/{role}/users/{userUuid}/token")
    fun getConfig(
        @Path("roomUuid") roomUuid: String,
        @Path("role") role: Int,
        @Path("userUuid") userUuid: String
    ): Call<ConfigResponse>

}