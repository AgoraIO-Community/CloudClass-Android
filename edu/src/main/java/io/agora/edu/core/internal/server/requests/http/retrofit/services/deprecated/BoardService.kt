package io.agora.edu.core.internal.server.requests.http.retrofit.services.deprecated

import io.agora.edu.core.internal.edu.common.bean.ResponseBody
import io.agora.edu.core.internal.edu.common.bean.board.sceneppt.BoardCoursewareRes
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface BoardService {
    @GET("edu/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/resources")
    fun getCourseware(
            @Path("appId") appId: String?,
            @Path("roomUuid") roomUuid: String?,
            @Path("userUuid") userUuid: String?
    ): Call<ResponseBody<List<BoardCoursewareRes?>?>?>?
}