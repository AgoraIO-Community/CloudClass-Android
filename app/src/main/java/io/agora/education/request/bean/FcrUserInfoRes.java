package io.agora.education.request.bean;


import android.text.TextUtils;

/**
 * author : felix
 * date : 2022/9/13
 * description :
 */
public class FcrUserInfoRes {
    public String companyId;
    public String companyName;
    public String userId;
    public String language;
    public String displayName;

    /**
     * 接口定义取的是：companyName
     *
     * @return
     */
    public String getUserName() {
        if (TextUtils.isEmpty(companyName)) {
            return displayName;
        }
        
        return companyName;
    }
}
