package io.agora.education.request.bean;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import io.agora.agoraeducore.core.internal.base.PreferenceManager;
import io.agora.agoraeducore.core.internal.education.impl.Constants;
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil;
import io.agora.agoraeducore.core.internal.launch.AgoraEduEnv;
import io.agora.agoraeducore.core.internal.launch.AgoraEduRegion;
import io.agora.agoraeducore.core.internal.util.FcrSceneUtils;
import io.agora.education.config.AppConstants;
import io.agora.education.request.AppUserInfoUtils;
import io.agora.education.utils.AppUtil;

/**
 * author : felix
 * date : 2022/9/15
 * description :
 * https://yapi.sh2.agoralab.co/project/47/interface/api/1764
 */
public class FcrRoomDetail {
    public String roomName;
    public String roomId;
    /**
     * 房间类型     [0：one_on_one  2：large_class  4：edu_medium_v1  6：proctoring]
     */
    @Deprecated
    public int roomType;
    /**
     * 课堂状态 [0：未开始     1：进行中  2：已结束]
     */
    public int roomState;
    public long startTime;
    public long endTime;
    /**
     * s
     */
    public long duration;

    public int role;

    public String userName;

    public FcrCreateRoomProperties roomProperties;

    /**
     * 场景类型
     * 0：one_on_one     roomType = 0
     * 2：large_class    roomType = 2
     * 4：edu_medium_v1  roomType = 4
     * 6：proctoring     roomType = 6
     * 10: cloud_class   roomType = 4
     */
    public int sceneType = 4;

    public int getRoomType() {
        return FcrSceneUtils.INSTANCE.getRoomType(sceneType);
    }

    public String getShareLink(Context context, String userName) {
        String shareLink = "User";

        String owner = "";
        try {
            owner = URLEncoder.encode(userName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            roomId = URLEncoder.encode(roomId, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        FcrShareData info = new FcrShareData();
        info.owner = owner;
        info.roomId = roomId;
        info.region = PreferenceManager.get(AppConstants.KEY_SP_REGION, AgoraEduRegion.cn);

        String data = GsonUtil.INSTANCE.toJson(info);

        // 2.8.10 ==> 2.8.1x
//        String version = AppUtil.getVersionName(context);
//        String[] code = version.split("\\.");
//        if (code.length < 3) {
//            Log.e("FcrRoomDetail", "version is error：" + version);
//            return "";
//        }
//        version = code[0] + "." + code[1];
//        String endVersion = code[2].substring(0, 1);

        try {
            String sc = Base64.encodeToString(data.getBytes("UTF-8"), Base64.DEFAULT);

            Log.i("FcrRoomDetail", "||sc=" + sc + "||data=" + data);

            shareLink = getShareUrl(sc);

            Log.i("FcrRoomDetail", "shareLink=" + shareLink);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return shareLink;
    }

    /**
     * @param sc
     * @return
     */
    public String getShareUrl(String sc) {
//        if (PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false) == true) {
//            AgoraEduEnv env = PreferenceManager.getObject(Constants.KEY_SP_ENV, AgoraEduEnv.class);
//            if (env == AgoraEduEnv.ENV) {
//                return "https://solutions-apaas.agora.io/apaas/app/test/release_" + version + "." + endVersion + "x/index.html#/invite?sc=" + sc;
//            }
//        }
        // 正式环境
        return "https://solutions-apaas.agora.io/apaas/demo/index.html#/invite?sc=" + sc;
    }
}
