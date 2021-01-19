package io.agora.education.api.user

import android.view.ViewGroup
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.stream.data.*
import io.agora.education.api.user.data.EduLocalUserInfo
import io.agora.education.api.user.data.EduActionConfig
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.listener.EduUserEventListener

interface EduUser {
    var userInfo: EduLocalUserInfo
    var videoEncoderConfig: VideoEncoderConfig

    var eventListener: EduUserEventListener?

    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message*/
    fun initOrUpdateLocalStream(options: LocalStreamInitOptions, callback: EduCallback<EduStreamInfo>)

    /**code:message
     * 201:media error:code，透传rtc错误code或者message*/
    fun switchCamera(): EduError?

    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message*/
    fun subscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message*/
    fun unSubscribeStream(stream: EduStreamInfo, options: StreamSubscribeOptions, callback: EduCallback<Unit>)

    /**新建流信息*/
    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message。
     * 301:network error，透传后台错误msg字段*/
    fun publishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    /**mute/unmute*/
    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message。
     * 301:network error，透传后台错误msg字段*/
    fun muteStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    /**删除流信息*/
    /**code:message
     * 1:parameter XXX is invalid
     * 201:media error:code，透传rtc错误code或者message。
     * 301:network error，透传后台错误msg字段*/
    fun unPublishStream(stream: EduStreamInfo, callback: EduCallback<Boolean>)

    /**发送自定义消息*/
    /**code:message
     * 1:parameter XXX is invalid
     * 301:network error，透传后台错误msg字段*/
    fun sendRoomMessage(message: String, callback: EduCallback<EduMsg>)

    /**
     * @param user 消息接收方的userInfo*/
    /**code:message
     * 1:parameter XXX is invalid
     * 301:network error，透传后台错误msg字段*/
    fun sendUserMessage(message: String, user: EduUserInfo, callback: EduCallback<EduMsg>)

    /**发送聊天消息*/
    /**code:message
     * 1:parameter XXX is invalid
     * 301:network error，透传后台错误msg字段*/
    fun sendRoomChatMessage(message: String, callback: EduCallback<EduChatMsg>)

    /**code:message
     * 1:parameter XXX is invalid
     * 301:network error，透传后台错误msg字段*/
    fun sendUserChatMessage(message: String, remoteUser: EduUserInfo, callback: EduCallback<EduChatMsg>)

    /*process action
    * 一期教育SDK没有这个方法，只是给娱乐使用*/
    fun startActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>)
    fun stopActionWithConfig(config: EduActionConfig, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid*/
    fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?,
                      config: VideoRenderConfig = VideoRenderConfig()): EduError

    /**code:message
     * 1:parameter XXX is invalid*/
    fun setStreamView(stream: EduStreamInfo, channelId: String, viewGroup: ViewGroup?): EduError

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun setRoomProperties(properties: MutableMap<String, Any>,
                          cause: MutableMap<String, String>, callback: EduCallback<Unit>)

    /**code:message
     * 1:parameter XXX is invalid
     * 2:internal error：可以内部订阅具体什么错误
     * 301:network error，透传后台错误msg字段*/
    fun removeRoomProperties(properties: MutableList<String>,
                             cause: MutableMap<String, String>, callback: EduCallback<Unit>)
}
