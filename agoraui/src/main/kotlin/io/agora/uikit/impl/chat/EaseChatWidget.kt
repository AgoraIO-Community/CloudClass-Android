package io.agora.uikit.impl.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import com.hyphenate.*
import com.hyphenate.chat.*
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.EaseIM
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.utils.SoftInputUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.InputMsgListener
import com.hyphenate.easeim.modules.view.`interface`.ViewClickListener
import com.hyphenate.easeim.modules.view.ui.widget.ChatViewPager
import com.hyphenate.easeim.modules.view.ui.widget.InputView
import com.hyphenate.util.EMLog
import io.agora.educontext.WidgetType
import io.agora.uikit.educontext.handlers.RoomHandler
import io.agora.uikit.impl.AgoraAbsWidget

class EaseChatWidget : AgoraAbsWidget(), InputMsgListener, ViewClickListener, ChatPagerListener {
    private var layout: View? = null
    private var mContext: Context? = null

    private var orgName = ""
    private var appName = ""
    private var appKey = ""

    private var userName = ""
    private var userUuid = ""
    private var mChatRoomId = ""
    private var nickName = ""
    private var avatarUrl = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image1.png"
    private var roomUuid = ""

    private var chatViewPager: ChatViewPager? = null
    private var frame: FrameLayout? = null
    private var inputView: InputView? = null
    private val softInputUtil = SoftInputUtil()

    companion object {
        private const val TAG = "EaseChatWidget"
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinedClassRoom() {
            EMLog.d(TAG, "Classroom joined success")
            addEaseIM()
        }
    }

    private fun addEaseIM() {
        // userName = localUserInfo.userUuid
        userName = getEduContext()?.userContext()?.localUserInfo()?.userUuid ?: ""
        userUuid = getEduContext()?.userContext()?.localUserInfo()?.userUuid ?: ""
        nickName = getEduContext()?.userContext()?.localUserInfo()?.userName ?: ""
        roomUuid = getEduContext()?.roomContext()?.roomInfo()?.roomUuid ?: ""

        parseProperties()
        chatViewPager = mContext?.let { ChatViewPager(it) }
        chatViewPager?.let {
            it.setAvatarUrl(avatarUrl)
            it.setChatRoomId(mChatRoomId)
            it.setNickName(nickName)
            it.setRoomUuid(roomUuid)
            it.setUserName(userName)
            it.setUserUuid(userUuid)
        }

        if (EaseIM.getInstance().init(mContext, appKey)) {
            frame?.addView(chatViewPager)
            chatViewPager?.loginIM()
            chatViewPager?.viewClickListener = this
            chatViewPager?.chatPagerListener = this
        }
        mContext?.let {
            inputView = InputView(it)
            val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            inputView!!.layoutParams = params
            getContainer()?.layout()?.addView(inputView)
            inputView!!.visibility = GONE
            inputView!!.inputMsgListener = this
            inputView!!.chatRoomId = mChatRoomId
            inputView!!.roomUuid = roomUuid
            inputView!!.nickName = nickName
            inputView!!.avatarUrl = avatarUrl
            softInputUtil.attachSoftInput(
                    inputView
            ) { isSoftInputShow, softInputHeight, viewOffset ->
                if (isSoftInputShow)
                    inputView!!.translationY = inputView!!.translationY - viewOffset
                else
                    inputView!!.translationY = 0F
            }
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
            frame = it.findViewById(R.id.fragment_container)
            parent.addView(layout)
            val params = it.layoutParams as ViewGroup.MarginLayoutParams
            params.width = width
            params.height = height
            params.leftMargin = left
            params.topMargin = top
//            val params = RelativeLayout.LayoutParams(width / 2, height / 3 * 2)
//            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
//            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            it.layoutParams = params
        }
    }

    override fun receive(fromCompId: String, cmd: String, vararg: Any?) {

    }

    override fun release() {

    }

    override fun setRect(rect: Rect) {

    }

    override fun onSendMsg() {
        chatViewPager?.refreshUI()
        inputView?.visibility = GONE
    }

    override fun onAnnouncementClick() {

    }

    override fun onMsgContentClick() {
        inputView?.visibility = VISIBLE
        inputView?.hideFaceView()
    }

    override fun onFaceIconClick() {
        inputView?.visibility = VISIBLE
        inputView?.showFaceView()
    }

    override fun onAllMemberMuted(isMuted: Boolean) {
        if (isMuted && inputView?.isVisible == true) {
            inputView?.editContent?.let { CommonUtil.hideSoftKeyboard(it) }
            inputView?.visibility = GONE
        }
    }

    override fun onSingleMuted(isMuted: Boolean) {
        if (isMuted && inputView?.isVisible == true) {
            inputView?.editContent?.let { CommonUtil.hideSoftKeyboard(it) }
            inputView?.visibility = GONE
        }
    }

    override fun onIconHideenClick() {

    }


}