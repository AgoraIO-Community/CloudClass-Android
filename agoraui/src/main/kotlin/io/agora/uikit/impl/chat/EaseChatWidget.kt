package io.agora.uikit.impl.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hyphenate.*
import com.hyphenate.chat.*
import com.hyphenate.easeim.EaseIM
import com.hyphenate.easeim.R
import com.hyphenate.easeim.constant.DemoConstant
import com.hyphenate.easeim.widget.ChatTotalLayout
import com.hyphenate.exceptions.HyphenateException
import com.hyphenate.util.EMLog
import io.agora.educontext.WidgetType
import io.agora.uikit.educontext.handlers.RoomHandler
import io.agora.uikit.impl.AgoraAbsWidget

class EaseChatWidget : AgoraAbsWidget() {
    private var layout: View? = null
    private var mContext: Context? = null

    private var orgName = ""
    private var appName = ""
    private var appKey = ""

    private var userName = ""
    private var userUuid = ""
    private var mChatRoomId = "148364667715585"
    private var nickName = "学生A"
    private var avatarUrl = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image1.png"
    private var roomUuid = ""

    private var loginLimit = 0
    private var joinLimit = 0

    private var messageListener: EMMessageListener? = null
    private var chatRoomChangeListener: EMChatRoomChangeListener? = null
    private var totalLayout: ChatTotalLayout? = null

    private val isAllMemberMuted: Unit
        get() {
            EMClient.getInstance().chatroomManager().asyncFetchChatRoomFromServer(
                    mChatRoomId, object : EMValueCallBack<EMChatRoom> {
                override fun onSuccess(value: EMChatRoom) {
                    if (value.isAllMemberMuted) {
                        totalLayout!!.sendHandleEnable(mContext!!.resources
                                .getString(R.string.total_silence), false)
                    } else {
                        totalLayout!!.sendHandleEnable(mContext!!.resources
                                .getString(R.string.send_danmaku), true)
                    }
                }

                override fun onError(error: Int, errorMsg: String) {

                }
            })
        }

    companion object {
        private const val TAG = "EaseChatWidget"
    }

    private val roomHandler = object : RoomHandler() {
        override fun onClassroomJoined() {
            EMLog.d(TAG, "Classroom joined success")
            joinEaseIM()
        }
    }

    private fun joinEaseIM() {
        loginLimit = 0
        joinLimit = 0

        // userName = localUserInfo.userUuid
        userName = getEduContext()?.userContext()?.localUserInfo()?.userUuid ?: ""
        userUuid = getEduContext()?.userContext()?.localUserInfo()?.userUuid ?: ""
        nickName = getEduContext()?.userContext()?.localUserInfo()?.userName ?: ""
        roomUuid = getEduContext()?.roomContext()?.roomInfo()?.roomUuid ?: ""

        parseProperties()

        totalLayout?.let {
            it.setAvatarUrl(avatarUrl)
            it.setChatRoomId(mChatRoomId)
            it.setNickName(nickName)
            it.setRoomUuid(roomUuid)
        }

        EaseIM.getInstance().init(mContext, appKey)
        initEaseListener()
        if (userName.isNotEmpty() && userUuid.isNotEmpty()) {
            // userUuid is pwd
            loginIM(userName, userUuid)
        } else {
            EMLog.e(TAG, "User name/uuid is null or empty, ease im login fail")
        }
    }

    private fun parseProperties() {
        val properties = getEduContext()?.widgetContext()?.getWidgetProperties(WidgetType.IM)
        properties?.let {
            orgName = it["orgName"] as? String ?: ""
            appName = it["appName"] as? String ?: ""
            mChatRoomId = it["chatRoomId"] as? String ?: mChatRoomId
            appKey = it["appKey"] as? String ?: ""
        }

        val userProps = getEduContext()?.userContext()?.localUserInfo()?.properties
        userProps?.let {
            avatarUrl = it["avatar"] ?: avatarUrl
        }
    }

    @SuppressLint("InflateParams")
    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {
        mContext = parent.context
        getEduContext()?.roomContext()?.addHandler(roomHandler)

        layout = LayoutInflater.from(mContext).inflate(
                R.layout.ease_chat_layout, null, false)

        layout?.let {
            totalLayout = it.findViewById(R.id.chat_total)
            parent.addView(layout)

            val params = it.layoutParams as ViewGroup.MarginLayoutParams
            params.width = width
            params.height = height
            params.leftMargin = left
            params.topMargin = top
            it.layoutParams = params
        }
    }

    private fun initEaseListener() {
        messageListener = object : EMMessageListener {
            override fun onMessageReceived(messages: List<EMMessage>) {
                for (message in messages) {
                    if (message.getIntAttribute(DemoConstant.MSG_TYPE, 0) == DemoConstant.MSG_TYPE_NORMAL) {
                        totalLayout!!.sendHandleMessage(message)
                    }
                }
            }

            override fun onCmdMessageReceived(messages: List<EMMessage>) {
                for (message in messages) {
                    val body = message.body as EMCmdMessageBody
                    val action = body.action()
                    if (action == DemoConstant.DEL_ACTION) {
                        val msgId = message.getStringAttribute(DemoConstant.MSG_ID, "")
                        totalLayout!!.sendHandleRemoveDanmaku(msgId)
                    }
                }
            }

            override fun onMessageRead(messages: List<EMMessage>) {

            }

            override fun onMessageDelivered(messages: List<EMMessage>) {

            }

            override fun onMessageRecalled(messages: List<EMMessage>) {

            }

            override fun onMessageChanged(message: EMMessage, change: Any) {

            }
        }

        EMClient.getInstance().chatManager().addMessageListener(messageListener)

        chatRoomChangeListener = object : EMChatRoomChangeListener {
            override fun onChatRoomDestroyed(roomId: String, roomName: String) {

            }

            override fun onMemberJoined(roomId: String, participant: String) {

            }

            override fun onMemberExited(roomId: String, roomName: String, participant: String) {

            }

            override fun onRemovedFromChatRoom(reason: Int, roomId: String, roomName: String, participant: String) {

            }

            override fun onMuteListAdded(chatRoomId: String, mutes: List<String>, expireTime: Long) {
                for (member in mutes) {
                    totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.you_have_been_silenced), false)
                }
            }

            override fun onMuteListRemoved(chatRoomId: String, mutes: List<String>) {
                for (member in mutes) {
                    totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.send_danmaku), true)
                }
            }

            override fun onWhiteListAdded(chatRoomId: String, whitelist: List<String>) {

            }

            override fun onWhiteListRemoved(chatRoomId: String, whitelist: List<String>) {

            }

            override fun onAllMemberMuteStateChanged(roomId: String, isMuted: Boolean) {
                if (mChatRoomId == roomId) {
                    if (isMuted) {
                        totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.total_silence), false)
                    } else {
                        totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.send_danmaku), true)
                    }
                }
            }

            override fun onAdminAdded(chatRoomId: String, admin: String) {

            }

            override fun onAdminRemoved(chatRoomId: String, admin: String) {

            }

            override fun onOwnerChanged(chatRoomId: String, newOwner: String, oldOwner: String) {

            }

            override fun onAnnouncementChanged(chatRoomId: String, announcement: String) {

            }
        }

        EMClient.getInstance().chatroomManager().addChatRoomChangeListener(chatRoomChangeListener)
    }

    private fun loginIM(userName: String, pwd: String) {
        loginLimit++
        totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.in_the_login), false)
        EMClient.getInstance().login(userName, pwd, object : EMCallBack {
            override fun onSuccess() {
                val info = EMUserInfo()
                info.nickName = nickName
                info.avatarUrl = avatarUrl
                info.ext = DemoConstant.ROLE_STUDENT.toString()
                EaseIM.getInstance().updateOwnInfo(info)
                joinChatRoom()
            }

            override fun onError(code: Int, error: String) {
                EMLog.e("Login:", "$code:$error")
                if (loginLimit == 2) {
                    totalLayout!!.sendHandleToast(mContext!!.resources.getString(R.string.login_failed))
                    totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.login_failed), false)
                    return
                }
                // 判断不存在去注册再登录
                if (code == EMError.USER_NOT_FOUND) {
                    loginLimit = 0
                    createIM(userName, pwd)
                } else {
                    loginIM(userName, pwd)
                }
            }

            override fun onProgress(progress: Int, status: String) {}
        })
    }

    private fun joinChatRoom() {
        joinLimit++
        totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.in_the_join), false)
        EMClient.getInstance().chatroomManager().joinChatRoom(mChatRoomId, object : EMValueCallBack<EMChatRoom?> {
            override fun onSuccess(value: EMChatRoom?) {
                EMLog.e("Login:", "join success")
                isAllMemberMuted
            }

            override fun onError(error: Int, errorMsg: String) {
                EMLog.e("Login:", "join  $error:$errorMsg")
                if (joinLimit == 2) {
                    totalLayout!!.sendHandleToast(mContext!!.resources.getString(R.string.join_failed))
                    totalLayout!!.sendHandleEnable(mContext!!.resources.getString(R.string.join_failed), false)
                    return
                }
                joinChatRoom()
            }
        })
    }

    private fun createIM(userName: String, pwd: String) {
        Thread {
            try {
                EMClient.getInstance().createAccount(userName, pwd)
                loginIM(userName, pwd)
            } catch (e: HyphenateException) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun receive(fromCompId: String, cmd: String, vararg: Any?) {

    }

    override fun release() {
        totalLayout?.cancelHandler()
        cancelEaseListener()
        logoutIM()
    }

    private fun cancelEaseListener() {
        EMClient.getInstance().chatManager().removeMessageListener(messageListener)
        EMClient.getInstance().chatroomManager().removeChatRoomListener(chatRoomChangeListener)
    }

    private fun logoutIM() {
        EMClient.getInstance().logout(false)
    }

    override fun setRect(rect: Rect) {

    }
}