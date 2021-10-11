package io.agora.edu.core.internal.log;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.agora.edu.core.internal.base.callback.Callback;
import io.agora.edu.core.internal.base.callback.ThrowableCallback;
import io.agora.edu.core.internal.base.network.RetrofitManager;
import io.agora.edu.core.internal.base.network.S3CallbackBody;
import io.agora.edu.core.internal.log.service.LogService;
import io.agora.edu.core.internal.log.service.bean.ResponseBody;
import io.agora.edu.core.internal.log.service.bean.response.LogParamsRes;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;

import static io.agora.edu.core.internal.log.UploadManager.Params.TYPE;

public class UploadManager {
    private static final String callbackPath = "/monitor/apps/{appId}/v1/log/oss/callback";
    private static final String APP_JSON = "application/json";
    private static Object object = new Object();

    static {
        Params.AndroidLog.put(TYPE, "Android-log");
        Params.AndroidException.put(TYPE, "Android-exception");
    }

    public static class Params {
        public static final String ZIP = "zip";
        public static final String LOG = "log";
        public static final String TYPE = "type";
        public static final Map<String, String> AndroidLog = new ArrayMap();
        public static final Map<String, String> AndroidException = new ArrayMap();
    }

    public static class UploadParam {
        public String appVersion;
        public String deviceName;
        public String deviceVersion;
        public String fileExt;
        public String platform;
        public Object tag;

        public UploadParam(
                @Nullable String appVersion,
                @Nullable String deviceName,
                @NonNull String deviceVersion,
                /**
                 * zip/log; 扩展名，如果传扩展名则以扩展名为准，如果不传，terminalType=3为log，其他为zip
                 */
                @Nullable String fileExt,
                @NonNull String platform,
                @Nullable Object tag
        ) {
            this.appVersion = appVersion;
            this.deviceName = deviceName;
            this.deviceVersion = deviceVersion;
            this.fileExt = fileExt;
            this.platform = platform;
            this.tag = tag;
        }
    }

    public static class UploadParamTag {
        public String roomUuid;
        public String roomName;
        public int roomType;
        public String userUuid;
        public String userName;
        public int role;

        public UploadParamTag(String roomUuid, String roomName,
                              int roomType, String userUuid, String userName, int role) {
            this.roomUuid = roomUuid;
            this.roomName = roomName;
            this.roomType = roomType;
            this.userUuid = userUuid;
            this.userName = userName;
            this.role = role;
        }
    }

    public static void upload(@NonNull Context context, @NonNull String appId,
                              @NonNull String host, @NonNull String uploadPath,
                              @NonNull UploadParam param, @Nullable ThrowableCallback<String> callback) {
        LogService service = RetrofitManager.instance().getService(host, LogService.class);
        service.logParams(appId, APP_JSON, param)
                .enqueue(new RetrofitManager.Callback<>(0, new ThrowableCallback<ResponseBody<LogParamsRes>>() {
                    @Override
                    public void onSuccess(ResponseBody<LogParamsRes> res) {
//                        res.data.callbackUrl = service.logStsCallback(host).request().url()
//                                .toString().concat(callbackPath.replace("{appId}", appId));
//                        new Thread(() -> {
//                            uploadByOss(context, uploadPath, res.data, callback);
//                        }).start();
                        if (res.data.vendor == 2) {
                            res.data.callbackUrl = res.data.callbackHost.concat(callbackPath.replace("{appId}", appId));
                            new Thread(() -> uploadByOss(context, uploadPath, res.data, callback)).start();
                        } else if (res.data.vendor == 1) {
                            new Thread(() -> uploadByS3(context, uploadPath, res.data, service, appId, callback)).start();
                        }else {
                            res.data.callbackUrl = service.logStsCallback(host).request().url()
                                    .toString().concat(callbackPath.replace("{appId}", appId));
                            new Thread(() -> uploadByOss(context, uploadPath, res.data, callback)).start();
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (callback != null) {
                            ((ThrowableCallback<String>) callback).onFailure(throwable);
                        }
                    }
                }));
    }

    private static void uploadByS3(@NonNull Context context, @NonNull String uploadPath,
                                   @NonNull LogParamsRes param, LogService service, @NonNull String appId, @Nullable Callback<String> callback) {

        //get the file and zip it
        File file = new File(new File(uploadPath).getParentFile(), "temp.zip");
        try {
            ZipUtils.zipFile(new File(uploadPath), file);//get the compressed file
        } catch (IOException e) {
            e.printStackTrace();
        }

        RequestBody requestFile =
                RequestBody.create(MediaType.parse("multipart/form-data"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);
        String preSignedUrl = param.preSignedUrl;
        //upload the zip file
        service.uploadToPreSigned(preSignedUrl, body).enqueue(new retrofit2.Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    String callbackBody = param.callbackBody;

                    S3CallbackBody callbackHost = new Gson().fromJson(callbackBody, S3CallbackBody.class);
                    service.postCallbackBody3(param.callbackHost.concat(callbackPath.replace("{appId}", appId)), callbackHost)
                            .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<String>>() {
                                @Override
                                public void onSuccess(@Nullable ResponseBody<String> res) {
                                    if (callback != null) {
                                        String data = res.data;
                                        callback.onSuccess(data);
                                    }
                                    Log.d("postCallbackBody", "upload success");
                                }

                                @Override
                                public void onFailure(@Nullable Throwable throwable) {
                                    Toast.makeText(context, "upload failed", Toast.LENGTH_SHORT).show();
                                    Log.d("postCallbackBody", "upload failed");
                                }
                            }));
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(context, "upload failed", Toast.LENGTH_SHORT).show();//failed sometimes
            }
        });
    }

    private static void uploadByOss(@NonNull Context context, @NonNull String uploadPath,
                                    @NonNull LogParamsRes param, @Nullable Callback<String> callback) {
        try {
            synchronized (object) {
                CountDownLatch countDownLatch = new CountDownLatch(1);

                File file = new File(new File(uploadPath).getParentFile(), "temp.zip");
                ZipUtils.zipFile(new File(uploadPath), file);

                // 构造上传请求。
                PutObjectRequest put = new PutObjectRequest(param.bucketName, param.ossKey, file.getAbsolutePath());
                put.setCallbackParam(new HashMap<String, String>() {{
                    put("callbackUrl", param.callbackUrl);
                    put("callbackBodyType", param.callbackContentType);
                    put("callbackBody", param.callbackBody);
                }});
                // 推荐使用OSSAuthCredentialsProvider。token过期可以及时更新。
                OSSCredentialProvider credentialProvider = new OSSStsTokenCredentialProvider(param.accessKeyId,
                        param.accessKeySecret, param.securityToken);
                OSS oss = new OSSClient(context, param.ossEndpoint, credentialProvider);
                oss.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
                    @Override
                    public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        countDownLatch.countDown();
                        if (callback != null) {
                            String body = result.getServerCallbackReturnBody();
                            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                            callback.onSuccess(json.get("data").getAsString());
                        }
                    }

                    @Override
                    public void onFailure(PutObjectRequest request, ClientException clientException, ServiceException serviceException) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                        countDownLatch.countDown();
                        if (callback instanceof ThrowableCallback) {
                            if (clientException != null) {
                                ((ThrowableCallback<String>) callback).onFailure(clientException);
                            } else if (serviceException != null) {
                                ((ThrowableCallback<String>) callback).onFailure(serviceException);
                            } else {
                                ((ThrowableCallback<String>) callback).onFailure(null);
                            }
                        }
                    }
                });
                countDownLatch.await(70 * 1000, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
