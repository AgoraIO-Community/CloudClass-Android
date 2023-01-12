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
import io.agora.education.config.AppConstants;
import io.agora.education.request.AppUserInfoUtils;
import io.agora.education.utils.AppUtil;

/**
 * author : hefeng
 * date : 2022/9/15
 * description :
 */
public class FcrRoomDetail {
    public String roomName;
    public String roomId;
    /**
     * 房间类型     [0：one_on_one  2：large_class  4：edu_medium_v1  6：proctoring]
     */
    public int roomType;
    /**
     * 课堂状态 [0：未开始     1：进行中  2：已结束]
     */
    public int roomState;
    public long startTime;
    public long endTime;
    public int role;

    public FcrCreateRoomProperties roomProperties;

    public String getShareLink(Context context) {
        String shareLink = "";

        if (AppUserInfoUtils.INSTANCE.getUserInfo() != null) {
            String owner = AppUserInfoUtils.INSTANCE.getUserInfo().getUserName();
            try {
                owner = URLEncoder.encode(owner, "utf-8");
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

            // 2.8.0
            String version = AppUtil.getVersionName(context);
            String[] code = version.split("\\.");
            version = code[0] + "." + code[1];

            try {
                String sc = Base64.encodeToString(data.getBytes("UTF-8"), Base64.DEFAULT);

                Log.e("hefeng", "version=" + version + "||sc=" + sc + "||data=" + data);

                shareLink = getShareUrl(version, sc);

                Log.e("hefeng", "shareLink=" + shareLink);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return shareLink;
    }

    /**
     * @param version
     * @param sc
     * @return
     */
    public String getShareUrl(String version, String sc) {
        if (PreferenceManager.get(Constants.KEY_SP_USE_OPEN_TEST_MODE, false) == true) {
            AgoraEduEnv env = PreferenceManager.getObject(Constants.KEY_SP_ENV, AgoraEduEnv.class);
            if (env == AgoraEduEnv.ENV) {
                return "https://solutions-apaas.agora.io/apaas/app/test/release_" + version + ".x/index.html#/invite?sc=" + sc;
            }
        }
        // 正式环境
        return "https://solutions-apaas.agora.io/apaas/app/prod/release_" + version + ".x/index.html#/invite?sc=" + sc;
    }
}
