package io.agora.education.request

import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.education.request.bean.*
import retrofit2.Call
import retrofit2.http.*

/**
 * author : felix
 * date : 2022/9/13
 * description : api
 */
interface AppService {
    /**拼接授权地址 RedirectUrl*/
    @POST("sso/v2/users/oauth/redirectUrl")
    fun getAuthWebPage(@Body redirectUrlReq: FcrRedirectUrlReq): Call<HttpBaseRes<String>>

    /**注销登录的账号*/
    @DELETE("sso/v2/users/logout")
    fun logOutAccount(): Call<HttpBaseRes<String>>

    /**
     * 获取用户信息
     */
    @GET("sso/v2/users/info")
    fun getUserInfo(): Call<HttpBaseRes<FcrUserInfoRes>>

    /**
     * 获取加入的课程列表
     */
    @GET("edu/companys/{companyId}/v1/rooms?count=10")
    fun getJoinRoomList(
        @Path("companyId") companyId: String,
        @QueryMap nextId: Map<String, String>
    ): Call<HttpBaseRes<FcrJoinListRoomRes>>

    /**
     * 创建教室
     */
    @POST("edu/companys/{companyId}/v1/rooms")
    fun createRoom(
        @Path("companyId") companyId: String,
        @Body req: FcrCreateRoomReq?
    ): Call<HttpBaseRes<FcrCreateRoomRes>>

    /**
     * 加入教室
     */
    @PUT("edu/companys/{companyId}/v1/rooms")
    fun joinRoom(
        @Path("companyId") companyId: String,
        @Body req: FcrJoinRoomReq?
    ): Call<HttpBaseRes<FcrJoinRoomRes>>

    /**
     * ip预检
     */
    @GET("edu/v1/preflight")
    fun requestIP(): Call<HttpBaseRes<FcrIPRes>>

    /**
     * 免登录：查询教室信息
     */
    @GET("edu/companys/v1/rooms/{roomUuid}")
    fun getRoomInfoForVisitor(
        @Path("roomUuid") roomUuid: String
    ): Call<HttpBaseRes<FcrRoomDetail>>

    /**
     * 免登录：加入教室
     */
    @PUT("edu/companys/v1/rooms")
    fun joinRoomForVisitor(
        @Body req: FcrJoinRoomReq?
    ): Call<HttpBaseRes<FcrJoinRoomRes>>

    /**
     * 免登录：创建教室
     */
    @POST("edu/companys/v1/rooms")
    fun createRoomForVisitor(
        @Body req: FcrCreateRoomReq?
    ): Call<HttpBaseRes<FcrCreateRoomRes>>
}