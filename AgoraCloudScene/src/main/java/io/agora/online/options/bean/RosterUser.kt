package io.agora.online.options.bean

import com.google.gson.annotations.SerializedName


/**
 * author : wufang
 * date : 2022/3/16
 * description :大班课 花名册请求返回信息
 */
data class Data(
    @SerializedName("total") var total: Int? = null,
    @SerializedName("list") var agoraEduRosterUserList: ArrayList<AgoraEduRosterUserInfo> = arrayListOf(),
    @SerializedName("pageNo") var pageNo: Int? = null,
    @SerializedName("pageSize") var pageSize: Int? = null,
    @SerializedName("pages") var pages: Int? = null
)

/**
 * author : wufang
 * date : 2022/3/16
 * description :大班课 花名册人员流信息
 */
data class AgoraEduRosterStreams(
    @SerializedName("streamUuid") var streamUuid: String? = null,
    @SerializedName("streamName") var streamName: String? = null,
    @SerializedName("videoSourceState") var videoSourceState: Int? = null,
    @SerializedName("videoSourceType") var videoSourceType: Int? = null,
    @SerializedName("videoState") var videoState: Int? = null,
    @SerializedName("audioSourceState") var audioSourceState: Int? = null,
    @SerializedName("audioSourceType") var audioSourceType: Int? = null,
    @SerializedName("audioState") var audioState: Int? = null,
    @SerializedName("updateTime") var updateTime: Long? = null,
    @SerializedName("state") var state: Int? = null
)

/**
 * author : wufang
 * date : 2022/3/16
 * description :大班课 花名册人员信息
 */
data class AgoraEduRosterUserInfo(
    @SerializedName("userName") var userName: String? = null,
    @SerializedName("userUuid") var userUuid: String? = null,
    @SerializedName("role") var role: String = "",
    @SerializedName("muteChat") var muteChat: Int? = null,
    @SerializedName("userProperties") var userProperties: UserProperties? = UserProperties(),
    @SerializedName("updateTime") var updateTime: Long? = null,
    @SerializedName("streamUuid") var streamUuid: String? = null,
    @SerializedName("state") var state: Int? = null,
    @SerializedName("streams") var streams: ArrayList<AgoraEduRosterStreams>? = arrayListOf()
)

data class EasemobIM(
    @SerializedName("userId") var userId: String? = null,
    @SerializedName("isAdmin") var isAdmin: Boolean? = null
)

data class FlexProps(
    @SerializedName("avatar") var avatar: String? = null

)


data class UserProperties(
    @SerializedName("flexProps") var flexProps: FlexProps? = FlexProps(),
    @SerializedName("widgets") var widgets: Widgets? = Widgets()
)


data class Widgets(
    @SerializedName("easemobIM") var easemobIM: EasemobIM? = EasemobIM()
)