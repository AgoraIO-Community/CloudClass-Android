package io.agora.edu.core.internal.log.service;

import androidx.annotation.NonNull;

import io.agora.edu.core.internal.base.network.S3CallbackBody;
import io.agora.edu.core.internal.log.UploadManager;
import io.agora.edu.core.internal.log.service.bean.ResponseBody;
import io.agora.edu.core.internal.log.service.bean.response.LogParamsRes;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Url;

public interface LogService {
    @POST("monitor/apps/{appId}/v1/log/oss/policy")
    Call<ResponseBody<LogParamsRes>> logParams(
            @Path("appId") @NonNull String appId,
            @Header("Content-Type") @NonNull String contentType,
            @Body @NonNull UploadManager.UploadParam param
    );

    @POST
    Call<ResponseBody<String>> logStsCallback(@Url String url);

    @Multipart
    @PUT
    Call<ResponseBody> uploadToPreSigned(@Url String url,
                                         @Part MultipartBody.Part file);

    @POST
    Call<ResponseBody<String>> postCallbackBody3(@Url String url,
                                                 @Body S3CallbackBody req);
}
