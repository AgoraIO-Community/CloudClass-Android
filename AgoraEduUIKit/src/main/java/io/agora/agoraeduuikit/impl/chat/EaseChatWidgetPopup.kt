package io.agora.agoraeduuikit.impl.chat

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMMessage
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
import com.hyphenate.easeim.modules.view.ui.widget.ShowImageView
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionSignal.UnreadTips
import io.agora.agoraeduuikit.util.AppUtil

class EaseChatWidgetPopup : ChatPopupWidget(), InputMsgListener, ChatPagerListener {
    override val tag = "EaseChatWidgetPopup"

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
    // specified input`s parentView
    private var specialInputViewParent: ViewGroup? = null
    private var showImageView: ShowImageView? = null
    private val softInputUtil = SoftInputUtil()
    private lateinit var parent: ViewGroup
    private var width: Int = 0
    private var height: Int = 0
    private var right: Int = 0
    private var bottom: Int = 0

    // Specially designed to avoid recycler view having a minus height
    private val minHeight = 40
    private val elevation = 0
    private lateinit var hideLayout: RelativeLayout
    private lateinit var unreadText: AppCompatTextView
    private var initLoginEaseIM = false
    private var closeable = true

    fun setInputViewParent(viewGroup: ViewGroup) {
        this.specialInputViewParent = viewGroup
    }

    private fun getInputViewParent(): ViewGroup? {
        return specialInputViewParent
    }

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
            }

            contentLayout?.addView(chatViewPager)
            chatViewPager?.chatPagerListener = this

            if (appKey.isNotEmpty() &&
                EaseIM.getInstance().init(mContext, appKey)) {
                EaseRepository.instance.isInit = true
                chatViewPager?.loginIM()
            } else {
                mContext?.let {
                    AgoraUIToast.error(context = it, text = mContext?.getString(
                        com.hyphenate.easeim.R.string.login_chat_failed) + "--" +
                        mContext?.getString(com.hyphenate.easeim.R.string.appKey_is_empty))
                }
            }

            mContext?.let {
                inputView = InputView(it)
                val params = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)

                val inputParent = getInputViewParent() ?: container?.parent
                inputView?.let { input ->
                    input.layoutParams = params
                    if (inputParent != null && inputParent is ViewGroup) {
                        inputParent.addView(input)
                        input.visibility = View.GONE
                        input.inputMsgListener = this
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

                showImageView = ShowImageView(it)
                showImageView?.let { image ->
                    val params = RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT)
                    image.layoutParams = params
                    if (inputParent != null && inputParent is ViewGroup) {
                        inputParent.addView(image)
                        image.chatPagerListener = this
                        image.visibility = View.GONE
                        image.setOnClickListener {
                            image.visibility = View.GONE
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
        return !TextUtils.isEmpty(userName) && !TextUtils.isEmpty(userUuid) && !TextUtils.isEmpty(orgName) && !TextUtils.isEmpty(appName)
            && !TextUtils.isEmpty(mChatRoomId) && !TextUtils.isEmpty(appKey)
    }

    @SuppressLint("InflateParams")
    override fun init(parent: ViewGroup, width: Int, height: Int, top: Int, left: Int) {
        super.init(parent, width, height, top, left)
        mContext = parent.context
        this.parent = parent

        LayoutInflater.from(mContext).inflate(
            R.layout.ease_chat_layout, null, false)?.let {
            layout = it
            contentLayout = it.findViewById(R.id.fragment_container)
            contentLayout?.clipToOutline = true
            contentLayout?.elevation = elevation.toFloat()

            hideLayout = it.findViewById(com.hyphenate.easeim.R.id.chat_hide_icon_layout)
            unreadText = it.findViewById(com.hyphenate.easeim.R.id.chat_unread_text)
            hideLayout.visibility = View.GONE

            val param = ViewGroup.MarginLayoutParams(width, height)
            param.topMargin = top
            param.leftMargin = left
            parent.addView(layout, param)
        }
        addEaseIM()
    }

    override fun onWidgetRoomPropertiesUpdated(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
                                               keys: MutableList<String>) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        if (properties.keys.contains(appNameKey) && properties.keys.contains(chatRoomIdKey) && !initLoginEaseIM) {
            contentLayout?.post { addEaseIM() }
        }
    }

    override fun release() {
        super.release()
        chatViewPager?.logout()
    }

    override fun setRect(rect: Rect) {
        if (::parent.isInitialized) {
            parent.post {
                layout?.let { layout ->
                    val top = rect.top
                    val left = rect.left
                    val width = rect.width()
                    val height = rect.height()

                    if (layout.parent != null) {
                        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
                        params.topMargin = top
                        params.leftMargin = left
                        params.width = width
                        params.height = height
                        layout.layoutParams = params
                    } else {
                        val params = ViewGroup.MarginLayoutParams(width, height)
                        params.topMargin = top
                        params.leftMargin = left
                        parent.addView(layout, params)
                    }
                }
            }
        }
    }

    override fun onSendMsg(content: String) {
        chatViewPager?.sendTextMessage(content)
        inputView?.visibility = View.GONE
    }

    override fun onOutsideClick() {
        inputView?.visibility = View.GONE
    }

    override fun onContentChange(content: String) {
        chatViewPager?.setInputContent(content)
    }

    override fun onSelectImage() {
        chatViewPager?.selectPicFromLocal()
        inputView?.visibility = View.GONE
    }

    override fun onImageClick(message: EMMessage) {
        showImageView?.loadImage(message)
        showImageView?.visibility = View.VISIBLE
    }

    override fun onCloseImage() {
        showImageView?.visibility = View.GONE
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
        chatWidgetListener?.onChatPopupWidgetClosed()
    }

    override fun onShowUnread(show: Boolean) {
        ThreadManager.instance.runOnMainThread {
            if (hideLayout.isVisible)
                unreadText.visibility = View.VISIBLE
            else
                unreadText.visibility = if (show) View.VISIBLE else View.GONE
        }
        if (show) {
            broadcasterUnreadTip(show)
        } else {
            val tmp = contentLayout?.let { AppUtil.isVisibleToUser(it, parent.id) } ?: false
            broadcasterUnreadTip(tmp)
        }
    }

    private fun broadcasterUnreadTip(show: Boolean) {
        val body = AgoraChatInteractionPacket(UnreadTips, show)
        sendMessage(Gson().toJson(body))
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

    override fun getLayout(): ViewGroup? {
        return layout as? ViewGroup
    }

    override fun setFullscreenRect(fullScreen: Boolean, rect: Rect) {
        // Not used in this implementation
    }

    override fun setFullDisplayRect(rect: Rect) {
        // Not used in this implementation
    }

    override fun show(show: Boolean) {
        // Not used in this implementation
    }

    override fun isShowing(): Boolean {
        // Not used in this implementation
        return false
    }

    override fun setClosable(closeable: Boolean) {
        if (chatViewPager != null) {
            chatViewPager!!.setCloseable(closeable)
        } else {
            this.closeable = closeable
        }
    }

    override fun setTabDisplayed(displayed: Boolean) {
        if (chatViewPager != null) {
            chatViewPager!!.setTabLayoutCloseable(closeable)
        } else {
            this.closeable = closeable
        }
    }
}