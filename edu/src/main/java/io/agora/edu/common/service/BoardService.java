package io.agora.edu.common.service;

import java.util.List;

import io.agora.edu.common.bean.board.BoardBean;
import io.agora.edu.common.bean.ResponseBody;
import io.agora.edu.common.bean.board.sceneppt.BoardCoursewareRes;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface BoardService {

    @GET("edu/apps/{appId}/v1/rooms/{roomUuid}/users/{userUuid}/resources")
    Call<ResponseBody<List<BoardCoursewareRes>>> getCourseware(
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid,
            @Path("userUuid") String userUuid
    );
}
