package io.agora.record;

import io.agora.edu.common.bean.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReplayService {

    @GET("/recording/apps/{appId}/v1/rooms/{roomId}/records")
    Call<ResponseBody<ReplayRes>> record(
            @Path("appId") String appId,
            @Path("roomId") String roomId,
            @Query("nextId") int nextId
    );
}
