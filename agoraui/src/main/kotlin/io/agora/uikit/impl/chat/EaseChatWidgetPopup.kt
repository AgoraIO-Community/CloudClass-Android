package io.agora.uikit.impl.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.hyphenate.easeim.modules.EaseIM
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.utils.SoftInputUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.InputMsgListener
import com.hyphenate.easeim.modules.view.`interface`.ViewClickListener
import com.hyphenate.easeim.modules.view.ui.widget.ChatViewPager
import com.hyphenate.easeim.modules.view.ui.widget.InputView
import io.agora.educontext.WidgetType
import io.agora.uikit.R
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.educontext.handlers.RoomHandler
import io.agora.uikit.impl.AgoraAbsWidget

class EaseChatWidgetPopup : AgoraAbsWidget(), InputMsgListener, ViewClickListener, ChatPagerListener {
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
    private var contentLayout: FrameLayout? = null
    private var inputView: InputView? = null
    private val softInputUtil = SoftInputUtil()

    private lateinit var parent: ViewGroup
    private var width: Int = 0
    private var height: Int = 0
    private var right: Int = 0
    private var bottom: Int = 0

    // Specially designed to avoid recycler view having a minus height
    private val minHeight = 40
    private val elevation = 0

    var hideIconSize = 0

    private val duration = 400L

    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var contentTopMargin: Int = 0
    private var contentLeftMargin: Int = 0
    private lateinit var hideLayout: RelativeLayout
    private lateinit var unreadText: AppCompatTextView

    var dismissRunnable: Runnable? = null

    companion object {
        private const val TAG = "EaseChatWidget"
    }

    private val roomHandler = object : RoomHandler() {
        override fun onJoinedClassRoom() {
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

        contentLayout?.addView(chatViewPager)
        chatViewPager?.viewClickListener = this
        chatViewPager?.chatPagerListener = this

        if (appKey.isNotEmpty() &&
            EaseIM.getInstance().init(mContext, appKey)) {
            EaseRepository.instance.isInit = true
            chatViewPager?.loginIM()
        } else {
            AgoraUIToastManager.showShort(mContext?.getString(
                com.hyphenate.easeim.R.string.login_chat_failed) + "--" +
                    mContext?.getString(com.hyphenate.easeim.R.string.appKey_is_empty))
        }

        mContext?.let {
            inputView = InputView(it)
            val params = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

            inputView?.let { input ->
                input.layoutParams = params
                getContainer()?.layout()?.addView(input)
                input.visibility = View.GONE
                input.inputMsgListener = this
                input.chatRoomId = mChatRoomId
                input.roomUuid = roomUuid
                input.nickName = nickName
                input.avatarUrl = avatarUrl
                softInputUtil.attachSoftInput(input) { isSoftInputShow, softInputHeight, viewOffset ->
                    if (isSoftInputShow)
                        input.translationY = input.translationY - viewOffset
                    else {
                        input.translationY = 0F
                        if (input.isNormalFace()) input.visibility = View.GONE
                    }
                }
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

    }

    fun initView(parent: ViewGroup, width: Int, height: Int, right: Int, bottom: Int) {
        this.parent = parent
        this.width = width
        this.height = height
        this.right = right
        this.bottom = bottom

        mContext = parent.context
        hideIconSize = parent.context.resources.getDimensionPixelSize(
            com.hyphenate.easeim.R.dimen.hide_icon_size)

        getEduContext()?.roomContext()?.addHandler(roomHandler)

        layout = LayoutInflater.from(mContext).inflate(
            R.layout.ease_chat_layout, null, false)

        layout?.let {
            contentLayout = it.findViewById(R.id.fragment_container)
            hideLayout = it.findViewById(com.hyphenate.easeim.R.id.chat_hide_icon_layout)
            unreadText = it.findViewById(com.hyphenate.easeim.R.id.chat_unread_text)
            hideLayout.visibility = View.GONE
            hideLayout.setOnClickListener {
                dismiss()
                dismissRunnable?.run()
            }

            contentLayout?.clipToOutline = true
            contentLayout?.elevation = elevation.toFloat()
            contentWidth = width
            contentHeight = height

            parent.addView(it)
            dismiss()
        }
    }

    override fun receive(fromCompId: String, cmd: String, vararg: Any?) {

    }

    override fun release() {

    }

    override fun setRect(rect: Rect) {
        layout?.post {
            val params = layout?.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            layout?.layoutParams = params
        }
    }

    override fun onSendMsg() {
        chatViewPager?.refreshUI()
        inputView?.visibility = View.GONE
    }

    override fun onOutsideClick() {
        inputView?.visibility = View.GONE
    }

    override fun onContentChange(content: String) {
        chatViewPager?.setInputContent(content)
    }

    override fun onAnnouncementClick() {

    }

    override fun onMsgContentClick() {
        inputView?.visibility = View.VISIBLE
        inputView?.hideFaceView()
    }

    override fun onFaceIconClick() {
        inputView?.visibility = View.VISIBLE
        inputView?.showFaceView()
    }

    override fun onMuted(isMuted: Boolean) {
        if (isMuted && inputView?.isVisible == true) {
            inputView?.editContent?.let { CommonUtil.hideSoftKeyboard(it) }
            inputView?.visibility = View.GONE
        }
    }

    override fun onIconHideenClick() {
        dismiss()
        dismissRunnable?.run()
    }

    override fun onShowUnread(show: Boolean) {
        ThreadManager.instance.runOnMainThread {
            if(hideLayout.isVisible)
                unreadText.visibility = View.VISIBLE
            else
                unreadText.visibility = if(show) View.VISIBLE else View.GONE
        }
    }

    fun show() {
        layout?.let { layout ->
            val params = layout.layoutParams as ViewGroup.MarginLayoutParams
            params.width = width
            params.height = height
            params.rightMargin = this.right
            params.bottomMargin = this.bottom
            params.leftMargin = parent.width - this.right - width
            params.topMargin = parent.height - this.bottom - height
            layout.layoutParams = params
        }
    }

    /**
     * Ease chat sdk logout and release all data and
     * resources of chat engine if the view is detached
     * from window. Here we set the size of window to
     * zero to dismiss chat widget layout
     */
    fun dismiss() {
        layout?.let { layout ->
            val param = layout.layoutParams as ViewGroup.MarginLayoutParams
            param.width = 0
            param.height = 0
            layout.layoutParams = param
        }
    }
}