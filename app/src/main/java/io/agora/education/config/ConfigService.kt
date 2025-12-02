package io.agora.education.config

import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ConfigService {

    @GET("edu/v3/rooms/{roomUuid}/roles/{role}/users/{userUuid}/token")
    fun getConfigV3(
        @Path("roomUuid") roomUuid: String,
        @Path("role") role: Int,
        @Path("userUuid") userUuid: String
    ): Call<HttpBaseRes<ConfigData>>

    /**
     * token007 + check token
     *
     * 获取token007(sso登陆)
     */
    @GET("edu/v4/rooms/{roomUuid}/roles/{role}/users/{userUuid}/token")
    fun getConfigV4(
        @Path("roomUuid") roomUuid: String,
        @Path("role") role: Int,
        @Path("userUuid") userUuid: String
    ): Call<HttpBaseRes<ConfigData>>

}