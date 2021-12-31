package io.agora.agoraeduuikit.impl.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.TextUtils.isEmpty
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.hyphenate.*
import com.hyphenate.chat.*
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
import io.agora.agoraeduuikit.component.toast.AgoraUIToast

class EaseChatWidget : ChatWidget(), InputMsgListener, ViewClickListener, ChatPagerListener {
    override val tag = "EaseChatWidget"
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
    private var left: Int = 0
    private var top: Int = 0

    // Specially designed to avoid recycler view having a minus height
    private val minHeight = 40
    private val elevation = 0
    private val duration = 400L
    private var contentWidth: Int = 0
    private var contentHeight: Int = 0
    private var contentTopMargin: Int = 0
    private var contentLeftMargin: Int = 0
    private lateinit var hideLayout: RelativeLayout
    private lateinit var unreadText: AppCompatTextView
    private var hidden = true
    private var closeable = true
    private var initLoginEaseIM = false

    private fun addEaseIM() {
        nickName = widgetInfo?.localUserInfo?.userName ?: ""
        roomUuid = widgetInfo?.roomInfo?.roomUuid ?: ""

        if (parseEaseConfigProperties()) {
            chatViewPager = mContext?.let { ChatViewPager(it) }
            chatViewPager?.let {
                it.setAvatarUrl(avatarUrl)
                it.setChatRoomId(mChatRoomId)
                it.setNickName(nickName)
                it.setRoomUuid(roomUuid)
                it.setUserName(userName)
                it.setUserUuid(userUuid)
                it.setCloseable(closeable)
            }
            contentLayout?.addView(chatViewPager)
            chatViewPager?.viewClickListener = this
            chatViewPager?.chatPagerListener = this
            if (appKey.isNotEmpty()) {
                if (EaseIM.getInstance().init(mContext, appKey)) {
                    EaseRepository.instance.isInit = true
                    chatViewPager?.loginIM()
                }
            } else {
                mContext?.let {
                    AgoraUIToast.error(context = it, text = mContext?.getString(
                        com.hyphenate.easeim.R.string.login_chat_failed) + "--" +
                        mContext?.getString(com.hyphenate.easeim.R.string.appKey_is_empty))
                }
            }
            mContext?.let {
                inputView = InputView(it)
                val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                inputView!!.layoutParams = params
                val inputParent = container?.parent
                if (inputParent != null && inputParent is ViewGroup) {
                    inputParent.addView(inputView)
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
                        else {
                            inputView!!.translationY = 0F
                            if (inputView!!.isNormalFace())
                                inputView!!.visibility = GONE
                        }
                    }
                }
            }
            initLoginEaseIM = true
        }
    }

    private fun parseEaseConfigProperties(): Boolean {
//        val properties = eduContext?.widgetContext2()?.getWidgetProperties(WidgetType.IM)
        val extraProperties = this.widgetInfo?.roomProperties as? MutableMap<*, *>
        extraProperties?.let {
            orgName = it["orgName"] as? String ?: ""
            appName = it["appName"] as? String ?: ""
            mChatRoomId = it["chatRoomId"] as? String ?: mChatRoomId
            appKey = it["appKey"] as? String ?: ""
        }

        this.widgetInfo?.localUserProperties?.let {
            userName = it[userIdKey].toString()
            userUuid = it[userIdKey].toString()
        }
        return !isEmpty(userName) && !isEmpty(userUuid) && !isEmpty(orgName) && !isEmpty(appName)
                && !isEmpty(mChatRoomId) && !isEmpty(appKey)
    }

    @SuppressLint("InflateParams")
    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {
        super.init(parent, width, height, top, left)
        this.parent = parent
        this.width = width
        this.height = height
        this.top = top
        this.left = left
        mContext = parent.context
        hideIconSize = parent.context.resources.getDimensionPixelSize(com.hyphenate.easeim.R.dimen.hide_icon_size)

        val layoutId = parent.context.resources.getIdentifier("ease_chat_layout", "layout",
                parent.context.packageName)
        layout = LayoutInflater.from(mContext).inflate(layoutId, null, false)
        layout?.let {
            val contentLayoutId = parent.context.resources.getIdentifier("fragment_container",
                    "id", parent.context.packageName)
            contentLayout = it.findViewById(contentLayoutId)
            hideLayout = it.findViewById(com.hyphenate.easeim.R.id.chat_hide_icon_layout)
            unreadText = it.findViewById(com.hyphenate.easeim.R.id.chat_unread_text)
            hideLayout.visibility = GONE
            hideLayout.setOnClickListener {
                if (hidden) {
                    showAnimate()
                }
            }

            contentLayout?.clipToOutline = true
            contentLayout?.elevation = elevation.toFloat()
            contentWidth = width
            contentHeight = height
            parent.addView(layout)
            val params = it.layoutParams as ViewGroup.MarginLayoutParams
            params.width = width
            params.height = height
            params.leftMargin = left
            params.topMargin = top
            it.layoutParams = params
        }
        addEaseIM()
    }

    override fun onWidgetRoomPropertiesUpdated(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        if (properties.keys.contains(appNameKey) && properties.keys.contains(chatRoomIdKey) && !initLoginEaseIM) {
            addEaseIM()
        }
    }

    override fun release() {
        super.release()
        EMClient.getInstance().logout(true)
    }

    private fun hideAnimate() {
        contentLayout?.animate()?.let { animate ->
            animate.setDuration(duration)
                    .scaleX(1.0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setUpdateListener {
                        if (contentLayout!!.height <= minHeight) {
                            return@setUpdateListener
                        }

                        val fraction = it.animatedFraction
                        val diffWidth = contentWidth * fraction
                        val diffHeight = contentHeight * fraction
                        val width = contentWidth - diffWidth
                        val height = contentHeight - diffHeight

                        val params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = width.toInt()
                        params.height = height.toInt()
                        params.leftMargin = diffWidth.toInt() + contentLeftMargin
                        params.topMargin = diffHeight.toInt() + contentTopMargin
                        contentLayout?.layoutParams = params

                        chatWidgetAnimateListener?.onChatWidgetAnimate(false, fraction, params.leftMargin,
                                params.topMargin, params.width, params.height)
                    }
                    .withStartAction { }
                    .withEndAction {
                        val params = layout?.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = hideIconSize
                        params.height = hideIconSize
                        params.leftMargin = this.left + this.width - hideIconSize
                        params.topMargin = this.top + this.height - hideIconSize
                        layout?.layoutParams = params

                        hideLayout.visibility = VISIBLE
                        contentLayout?.visibility = GONE
                        hidden = true
                    }
        }

    }

    private fun showAnimate() {
        contentLayout?.animate()?.let { animate ->
            animate.setDuration(duration)
                    .scaleX(1.0f)
                    .setInterpolator(DecelerateInterpolator())
                    .setUpdateListener {
                        val fraction = it.animatedFraction
                        val diffWidth = contentWidth * fraction
                        val diffHeight = contentHeight * fraction
                        val left = contentWidth - diffWidth + contentLeftMargin
                        val top = contentHeight - diffHeight + contentTopMargin

                        if (diffHeight <= minHeight) {
                            return@setUpdateListener
                        }

                        var params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = diffWidth.toInt()
                        params.height = diffHeight.toInt()
                        params.leftMargin = left.toInt()
                        params.topMargin = top.toInt()
                        contentLayout?.layoutParams = params

                        chatWidgetAnimateListener?.onChatWidgetAnimate(true, fraction, params.leftMargin,
                                params.topMargin, params.width, params.height)
                    }
                    .withStartAction {
                        chatViewPager?.showOuterLayerUnread()
                        hideLayout.visibility = GONE
                        contentLayout?.visibility = VISIBLE

                        val params = layout?.layoutParams as ViewGroup.MarginLayoutParams
                        params.width = width
                        params.height = height
                        params.topMargin = top
                        params.leftMargin = left
                        layout?.layoutParams = params
                    }
                    .withEndAction {
                        hidden = false
                    }
        }
    }

    /**
     * Directly show the chat window, and display the last item
     * of the chat list
     */
    override fun show(show: Boolean) {
        layout?.post {

            if (show) {
                var params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = contentWidth
                params.height = contentHeight
                params.leftMargin = contentLeftMargin
                params.topMargin = contentTopMargin
                contentLayout?.layoutParams = params

                params = layout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = width
                params.height = height
                params.topMargin = top
                params.leftMargin = left
                layout?.layoutParams = params

                hideLayout.visibility = GONE
                contentLayout?.visibility = VISIBLE
                hidden = false
            } else {
                var params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = 0
                params.height = minHeight
                params.leftMargin = contentLeftMargin
                params.topMargin = contentTopMargin
                contentLayout?.layoutParams = params

                params = layout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = hideIconSize
                params.height = hideIconSize
                params.leftMargin = this.left + this.width - hideIconSize
                params.topMargin = this.top + this.height - hideIconSize
                layout?.layoutParams = params

                hideLayout.visibility = VISIBLE
                contentLayout?.visibility = GONE

                hidden = true
            }
        }
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

    override fun setFullscreenRect(fullScreen: Boolean, rect: Rect) {
        setRect(rect)

        layout?.post {
            if (fullScreen) {
                val params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = 0
                params.height = minHeight
                contentLayout?.layoutParams = params
                contentLayout?.visibility = GONE
                hideLayout.visibility = VISIBLE
                hidden = true
            } else {
                val params = contentLayout?.layoutParams as ViewGroup.MarginLayoutParams
                params.width = contentWidth
                params.height = contentHeight
                params.topMargin = contentTopMargin
                params.leftMargin = contentLeftMargin
                contentLayout?.layoutParams = params
                hideLayout.visibility = GONE
                contentLayout?.visibility = VISIBLE
                hidden = false
            }
        }
    }

    /**
     * * Called when the size of unfold chat window is changed
     */
    override fun setFullDisplayRect(rect: Rect) {
        this.width = rect.width()
        this.height = rect.height()
        this.contentWidth = width
        this.contentHeight = height
        this.left = rect.left
        this.top = rect.top
    }

    override fun isShowing(): Boolean {
        return !hidden
    }

    @UiThread
    override fun setClosable(closeable: Boolean) {
        if (chatViewPager != null) {
            chatViewPager!!.setCloseable(closeable)
        } else {
            this.closeable = closeable
        }
    }

    override fun onSendMsg() {
        chatViewPager?.refreshUI()
        inputView?.visibility = GONE
    }

    override fun onOutsideClick() {
        inputView?.visibility = GONE
    }

    override fun onContentChange(content: String) {
        chatViewPager?.setInputContent(content)
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

    override fun onMuted(isMuted: Boolean) {
        if (isMuted && inputView?.isVisible == true) {
            inputView?.editContent?.let { CommonUtil.hideSoftKeyboard(it) }
            inputView?.visibility = GONE
        }
    }

    override fun onIconHideenClick() {
        if (!hidden) {
            hideAnimate()
        }
    }

    override fun onShowUnread(show: Boolean) {
        ThreadManager.instance.runOnMainThread {
            if (hideLayout.isVisible)
                unreadText.visibility = VISIBLE
            else
                unreadText.visibility = if (show) VISIBLE else GONE
        }
    }
}