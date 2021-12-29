package io.agora.education;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;

import io.agora.agoraclasssdk.AgoraClassSdk;
import io.agora.agoraclasssdk.AgoraClassSdkConfig;
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback;
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager;
import io.agora.agoraeducore.core.internal.download.db.DbHolder;
import io.agora.agoraeducore.core.internal.edu.classroom.EduDebugMode;
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody;
import io.agora.agoraeducore.core.internal.edu.common.bean.board.sceneppt.BoardCoursewareRes;
import io.agora.agoraeducore.core.internal.edu.common.bean.board.sceneppt.SceneInfo;
import io.agora.agoraeducore.core.internal.launch.AgoraEduLatencyLevel;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion;
import io.agora.agoraeducore.core.internal.launch.AgoraEduStreamState;
import io.agora.agoraeducore.core.internal.launch.AgoraEduCourseware;
import io.agora.agoraeducore.core.internal.launch.AgoraEduCoursewarePreloadListener;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEvent;
import io.agora.agoraeducore.core.internal.launch.AgoraEduLaunchConfig;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoomType;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK;
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDKConfig;
import io.agora.agoraeducore.core.internal.launch.AgoraEduStreamStatus;
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.deprecated.BoardService;
import io.agora.agoraeducore.core.internal.util.FileUtil;
import io.agora.agoraeducore.core.internal.util.TimeUtil;
import io.agora.education.rtmtoken.RtmTokenBuilder;
import static io.agora.education.Constants.KEY_SP;
import static io.agora.education.EduApplication.getAppId;

public class QAActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE_RTC = 101;

    @BindView(R.id.et_room_name)
    protected EditText et_room_name;
    @BindView(R.id.et_room_uuid)
    protected EditText et_room_uuid;
    @BindView(R.id.et_your_name)
    protected EditText et_your_name;
    @BindView(R.id.et_your_uuid)
    protected EditText et_your_uuid;
    @BindView(R.id.et_room_type)
    protected EditText et_room_type;
    @BindView(R.id.card_room_type)
    protected CardView card_room_type;
    @BindView(R.id.et_room_region)
    protected EditText et_room_region;
    @BindView(R.id.card_room_region)
    protected CardView card_room_region;
    @BindView(R.id.btn_join)
    protected Button btnJoin;
    @BindView(R.id.timePicker)
    protected TimePicker timePicker;
    @BindView(R.id.durationEt)
    protected EditText durationEt;
    @BindView(R.id.configText)
    protected AppCompatTextView configText;
    @BindView(R.id.loadText)
    protected AppCompatTextView loadText;
    @BindView(R.id.clearCacheText)
    protected AppCompatTextView clearCacheText;

    private String rtmToken;
    private AgoraEduCourseware courseware;

    private ForbiddenDialog mDialog;

    private final static String DYNAMIC_URL = "https://convertcdn.netless.link/dynamicConvert/%s.zip";
    private final static String DYNAMIC_URL1 = "https://%s/dynamicConvert/%s.zip";
    private final static String STATIC_URL = "https://convertcdn.netless.link/staticConvert/%s.zip";
    private final static String PUBLIC_FILE_URL = "https://convertcdn.netless.link/publicFiles.zip";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot()) {
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        calibrateTimestamp();
        timePicker.setIs24HourView(true);
        Date date = new Date(System.currentTimeMillis() + 2 * 60 * 1000);
        timePicker.setCurrentHour(date.getHours());
        timePicker.setCurrentMinute(date.getMinutes());
    }

    private void calibrateTimestamp() {
        RetrofitManager.instance().getService("https://api-solutions-dev.bj2.agoralab.co", BoardService.class)
                .getCourseware("f488493d1886435f963dfb3d95984fd4", "courseware0", "liyang1")
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<List<BoardCoursewareRes>>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<List<BoardCoursewareRes>> res) {
                        if (res != null) {
                            TimeUtil.calibrateTimestamp(res.ts);
                            ContextCompat.getMainExecutor(QAActivity.this).execute(new Runnable() {
                                @Override
                                public void run() {
                                    Date date = new Date(TimeUtil.currentTimeMillis() + 2 * 60 * 1000);
                                    timePicker.setCurrentHour(date.getHours());
                                    timePicker.setCurrentMinute(date.getMinutes());
                                }
                            });
                        }
                    }
                }));
    }

    private void requestCourseware(String resourceUuid, ThrowableCallback<AgoraEduCourseware> callback) {
        RetrofitManager.instance().getService("https://api-solutions-dev.bj2.agoralab.co", BoardService.class)
                .getCourseware("f488493d1886435f963dfb3d95984fd4", "courseware0", "liyang1")
                .enqueue(new RetrofitManager.Callback(0, new ThrowableCallback<ResponseBody<List<BoardCoursewareRes>>>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        Log.e(TAG, "request courseware failed!");
                        throwable.printStackTrace();
                        notifyBtnJoinEnable(true);
                        callback.onFailure(throwable);
                    }

                    @Override
                    public void onSuccess(@Nullable ResponseBody<List<BoardCoursewareRes>> res) {
                        try {
                            if (res.data != null && res.data.size() > 0) {
                                BoardCoursewareRes wareRes = res.data.get(0);
                                String scenePath, url;
                                for (BoardCoursewareRes ware : res.data) {
                                    if (ware.getResourceUuid().equals(resourceUuid)) {
                                        wareRes = ware;
                                    }
                                }
                                if (wareRes.getConversion().getType().equals("dynamic")) {
                                    url = String.format(DYNAMIC_URL, wareRes.getTaskUuid());
                                } else if (wareRes.getConversion().getType().equals("static")) {
                                    url = String.format(STATIC_URL, wareRes.getTaskUuid());
                                } else {
                                    url = PUBLIC_FILE_URL;
                                }
                                List<SceneInfo> scenes = wareRes.getTaskProgress().getConvertedFileList();
                                scenePath = File.separator.concat(wareRes.getResourceName())
                                        .concat(scenes.get(0).getName());
                                AgoraEduCourseware courseware = new AgoraEduCourseware(
                                        wareRes.getResourceName(), wareRes.getResourceUuid(),
                                        scenePath, scenes, url);
                                callback.onSuccess(courseware);
                            } else {
                                Log.e(TAG, "request courseware failed, response data is null!");
                                callback.onFailure(new Throwable("request courseware failed, response data is null!"));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "request courseware failed, parse response data error!");
                            callback.onFailure(new Throwable("request courseware failed, parse response data error!"));
                            e.printStackTrace();
                        }
                    }
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        int eyeCare = PreferenceManager.get(KEY_SP, false) ? 1 : 0;
        AgoraEduSDK.setConfig(new AgoraEduSDKConfig(getAppId(), eyeCare));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        switch (requestCode) {
            case REQUEST_CODE_RTC:
                start();
                break;
            default:
                break;
        }
    }

    @OnClick({R.id.iv_setting, R.id.et_room_type, R.id.btn_join,
            R.id.tv_one2one, R.id.tv_small_class, R.id.tv_large_class,
            R.id.tv_debug,
            R.id.config, R.id.load, R.id.clearCache, R.id.tv_cn,
            R.id.tv_na, R.id.tv_eu, R.id.tv_ap})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_setting:
                startActivity(new Intent(this, SettingActivity2.class));
                break;
            case R.id.btn_join:
                if (AppUtil.isFastClick()) {
                    return;
                }
                if (AppUtil.checkAndRequestAppPermission(this, new String[]{
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CODE_RTC)) {
                    start();
                }
                break;
            case R.id.tv_one2one:
                et_room_type.setText(R.string.one2one_class);
                card_room_type.setVisibility(View.GONE);
                break;
            case R.id.tv_small_class:
                et_room_type.setText(R.string.small_class);
                card_room_type.setVisibility(View.GONE);
                break;
            case R.id.tv_large_class:
                et_room_type.setText(R.string.large_class);
                card_room_type.setVisibility(View.GONE);
                break;
//            case R.id.tv_breakout_class:
//                et_room_type.setText(R.string.breakout);
//                card_room_type.setVisibility(View.GONE);
//                break;
//            case R.id.tv_intermediate_class:
//                et_room_type.setText(R.string.intermediate);
//                card_room_type.setVisibility(View.GONE);
//                break;
            case R.id.tv_debug:
                et_room_type.setText(R.string.debug);
                card_room_type.setVisibility(View.GONE);
                break;
            case R.id.config:
                if (AppUtil.isFastClick()) {
                    return;
                }
                requestCourseware("large", new ThrowableCallback<AgoraEduCourseware>() {
                    @Override
                    public void onFailure(@Nullable Throwable throwable) {
                        runOnUiThread(() -> configText.setText("课件配置失败"));
                    }

                    @Override
                    public void onSuccess(@Nullable AgoraEduCourseware ware) {
                        if (ware != null) {
                            List<AgoraEduCourseware> wares = new ArrayList<>();
                            wares.add(ware);
//                            AgoraClassSdk.INSTANCE.configCourseWare(wares);
                            runOnUiThread(() -> configText.setText("课件配置成功"));
                        } else {
                            runOnUiThread(() -> configText.setText("课件配置失败"));
                        }
                    }
                });
                break;
            case R.id.load:
                if (AppUtil.isFastClick()) {
                    return;
                }
                try {
//                    AgoraClassSdk.INSTANCE.downloadCourseWare(QAActivity.this, new AgoraEduCoursewarePreloadListener() {
//                        @Override
//                        public void onStartDownload(@NotNull AgoraEduCourseware ware) {
//                            Log.e(TAG, "onStartDownload->" + ware.getResourceUrl());
//                        }
//
//                        @Override
//                        public void onProgress(@NotNull AgoraEduCourseware ware, double progress) {
//                            Log.e(TAG, "onProgress->" + progress);
//                            progress = progress * 100;
//                            int tmp = (int) progress;
//                            runOnUiThread(() -> loadText.setText(String.format("下载进度:%d%%", tmp)));
//                        }
//
//                        @Override
//                        public void onComplete(@NotNull AgoraEduCourseware ware) {
//                            Log.e(TAG, "onComplete->" + ware.getResourceUrl());
//                            runOnUiThread(() -> loadText.setText(String.format("下载进度完成")));
//                        }
//
//                        @Override
//                        public void onFailed(@NotNull AgoraEduCourseware ware) {
//                            Log.e(TAG, "onFailed->" + ware.getResourceUrl());
//                            runOnUiThread(() -> loadText.setText(String.format("下载进度失败")));
//                        }
//                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.clearCache:
                try {
                    File file = new File(QAActivity.this.getFilesDir().getAbsolutePath(), "board");
                    FileUtil.deleteDirectory(file.getAbsolutePath());
                    DbHolder dbHolder = new DbHolder(QAActivity.this);
                    dbHolder.clear();
                    dbHolder.close();
                    clearCacheText.setText("缓存清理成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.tv_cn:
                et_room_region.setText(R.string.cn0);
                card_room_region.setVisibility(View.GONE);
                break;
            case R.id.tv_na:
                et_room_region.setText(R.string.na0);
                card_room_region.setVisibility(View.GONE);
                break;
            case R.id.tv_eu:
                et_room_region.setText(R.string.eu0);
                card_room_region.setVisibility(View.GONE);
                break;
            case R.id.tv_ap:
                et_room_region.setText(R.string.ap0);
                card_room_region.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    @OnTouch({R.id.et_room_type, R.id.et_room_region})
    public void onTouch(View view, MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_UP) {
            return;
        }
        if (view.getId() == R.id.et_room_type) {
            if (card_room_type.getVisibility() == View.GONE) {
                card_room_type.setVisibility(View.VISIBLE);
                card_room_region.setVisibility(View.GONE);
            } else {
                card_room_type.setVisibility(View.GONE);
            }
        } else if (view.getId() == R.id.et_room_region) {
            if (card_room_region.getVisibility() == View.GONE) {
                card_room_region.setVisibility(View.VISIBLE);
                card_room_type.setVisibility(View.GONE);
            } else {
                card_room_region.setVisibility(View.GONE);
            }
        }
    }

    private void start() {
        notifyBtnJoinEnable(false);

        String roomName = et_room_name.getText().toString();
        if (TextUtils.isEmpty(roomName)) {
            Toast.makeText(this, R.string.room_name_should_not_be_empty, Toast.LENGTH_SHORT).show();
            notifyBtnJoinEnable(true);
            return;
        }

        String roomUuid = et_room_uuid.getText().toString();
        if (TextUtils.isEmpty(roomUuid)) {
            Toast.makeText(this, R.string.room_uuid_should_not_be_empty, Toast.LENGTH_SHORT).show();
            notifyBtnJoinEnable(true);
            return;
        }

        String userName = et_your_name.getText().toString();
        if (TextUtils.isEmpty(userName)) {
            Toast.makeText(this, R.string.your_name_should_not_be_empty, Toast.LENGTH_SHORT).show();
            notifyBtnJoinEnable(true);
            return;
        }

        String userUuid = et_your_uuid.getText().toString();
        if (TextUtils.isEmpty(userUuid)) {
            Toast.makeText(this, R.string.your_uuid_should_not_be_empty, Toast.LENGTH_SHORT).show();
            notifyBtnJoinEnable(true);
            return;
        }

        String type = et_room_type.getText().toString();
        if (TextUtils.isEmpty(type)) {
            Toast.makeText(this, R.string.room_type_should_not_be_empty, Toast.LENGTH_SHORT).show();
            notifyBtnJoinEnable(true);
            return;
        }
        int roomType = getRoomType(type);

        String region = et_room_region.getText().toString();
        if (TextUtils.isEmpty(region)) {
            region = et_room_region.getHint().toString();
        }
        region = getRoomRegion(region);

        AgoraEduStreamState streamState = new AgoraEduStreamState(AgoraEduStreamStatus.Enabled.getValue(),
                AgoraEduStreamStatus.Enabled.getValue());

        int roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.getValue();
        /*根据userUuid和appId签发的token*/
        rtmToken = "";

        /**本地生成rtmToken---开源版本*/
        try {
            // Agora app id, obtained from agora.io console
            String appId = getAppId();

            // Agora certificate, obtained from agora.io console
            String appCertificate = getString(R.string.agora_app_cert);

            // Open-source rtm token generation
            rtmToken = new RtmTokenBuilder().buildToken(appId, appCertificate, userUuid,
                    RtmTokenBuilder.Role.Rtm_User, 0);

            Date date1 = new Date();
            date1.setHours(timePicker.getCurrentHour());
            date1.setMinutes(timePicker.getCurrentMinute());
            long startTime = date1.getTime();
            if (startTime <= TimeUtil.currentTimeMillis()) {
                Toast.makeText(this, "开始时间必须是未来的某个时间", Toast.LENGTH_LONG).show();
                notifyBtnJoinEnable(true);
                return;
            }

            long duration = 310L;
            if (durationEt.length() > 0) {
                duration = Long.parseLong(durationEt.getText().toString());
            }

            AgoraClassSdk.INSTANCE.setConfig(new AgoraClassSdkConfig(appId));
            AgoraEduLaunchConfig launchConfig = new AgoraEduLaunchConfig(
                    userName, userUuid, roomName, roomUuid, roleType,
                    roomType, rtmToken, startTime, duration, region, null, null,
                    streamState, AgoraEduLatencyLevel.AgoraEduLatencyLevelUltraLow,
                    null, null);

            EduDebugMode.INSTANCE.setUseDebugUI(true);
            Log.d(TAG, "debug ui mode set to true");

            AgoraClassSdk.INSTANCE.launch(this, launchConfig, state -> {
                Log.e(TAG, ":launch-课堂状态:" + state.name());

                runOnUiThread(() -> {
                    notifyBtnJoinEnable(true);
                    if (state == AgoraEduEvent.AgoraEduEventForbidden) {
                        mDialog = new ForbiddenDialogBuilder(QAActivity.this)
                                .title(getResources().getString(R.string.join_forbidden_title))
                                .message(getResources().getString(R.string.join_forbidden_message))
                                .positiveText(getResources().getString(R.string.join_forbidden_button_confirm))
                                .positiveClick(view -> {
                                    if (mDialog != null && mDialog.isShowing()) {
                                        mDialog.dismiss();
                                        mDialog = null;
                                    }
                                })
                                .build();
                        mDialog.show();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void notifyBtnJoinEnable(boolean enable) {
        runOnUiThread(() -> {
            if (btnJoin != null) {
                btnJoin.setEnabled(enable);
            }
        });
    }

    private int getRoomType(String typeName) {
        if (typeName.equals(getString(R.string.one2one_class))) {
            return AgoraEduRoomType.AgoraEduRoomType1V1.getValue();
        } else if (typeName.equals(getString(R.string.small_class))) {
            return AgoraEduRoomType.AgoraEduRoomTypeSmall.getValue();
        } else if (typeName.equals(getString(R.string.large_class))) {
            return AgoraEduRoomType.AgoraEduRoomTypeBig.getValue();
        } else {
            return AgoraEduRoomType.AgoraEduRoomTypeSmall.getValue();
        }
    }

    private String getRoomRegion(String region) {
        if (region.equals(getString(R.string.cn0))) {
            return AgoraEduRegion.cn;
        } else if (region.equals(getString(R.string.na0))) {
            return AgoraEduRegion.na;
        } else if (region.equals(getString(R.string.eu0))) {
            return AgoraEduRegion.eu;
        } else if (region.equals(getString(R.string.ap0))) {
            return AgoraEduRegion.ap;
        } else {
            return AgoraEduRegion.cn;
        }
    }

    public void finish() {
        super.finish();
        EduDebugMode.INSTANCE.setUseDebugUI(false);
        Log.d(TAG, "debug ui mode set to false");
    }
}
