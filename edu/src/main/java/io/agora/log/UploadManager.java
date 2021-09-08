package io.agora.log;

import android.content.Context;

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
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.agora.base.callback.Callback;
import io.agora.base.callback.ThrowableCallback;
import io.agora.base.network.RetrofitManager;
import io.agora.log.service.LogService;
import io.agora.log.service.bean.ResponseBody;
import io.agora.log.service.bean.response.LogParamsRes;

import static io.agora.log.UploadManager.Params.TYPE;

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
                        res.data.callbackUrl = service.logStsCallback(host).request().url()
                                .toString().concat(callbackPath.replace("{appId}", appId));
                        new Thread(() -> {
                            uploadByOss(context, uploadPath, res.data, callback);
                        }).start();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        if (callback instanceof ThrowableCallback) {
                            ((ThrowableCallback<String>) callback).onFailure(throwable);
                        }
                    }
                }));
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
