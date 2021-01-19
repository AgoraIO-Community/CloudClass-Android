package io.agora.edu.common.service;

import io.agora.edu.common.bean.board.BoardBean;
import io.agora.edu.common.bean.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface BoardService {

    @GET("/board/apps/{appId}/v1/rooms/{roomUuid}")
    Call<ResponseBody<BoardBean>> getBoardInfo(
            @Header("token") String userToken,
            @Path("appId") String appId,
            @Path("roomUuid") String roomUuid
    );

}
