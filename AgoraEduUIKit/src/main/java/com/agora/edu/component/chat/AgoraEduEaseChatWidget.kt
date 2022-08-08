package com.agora.edu.component.chat

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import com.google.gson.Gson
import com.hyphenate.easeim.modules.EaseIM
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.utils.SoftInputUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.`interface`.InputMsgListener
import com.hyphenate.easeim.modules.view.ui.widget.ChatViewPager
import com.hyphenate.easeim.modules.view.ui.widget.InputView
import com.hyphenate.easeim.modules.view.ui.widget.ShowImageView
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.toast.AgoraUIToast
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionPacket
import io.agora.agoraeduuikit.impl.chat.AgoraChatInteractionSignal.UnreadTips
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidget
import io.agora.chat.ChatMessage

/**
 * 环信聊天组件
 * hyphenate chat widget
 */
class AgoraEduEaseChatWidget : ChatPopupWidget(), InputMsgListener, ChatPagerListener {
    override val tag = "EaseChatWidgetPopup"

    private var layout: View? = null
    private var mContext: Context? = null
    private var orgName = ""
    private var appName = ""
    private var appKey = ""
    private var role = EaseConstant.ROLE_STUDENT
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

    // Specially designed to avoid recycler view having a minus height
    private val elevation = 0
    private lateinit var hideLayout: RelativeLayout
    private lateinit var unreadText: AppCompatTextView
    private var initLoginEaseIM = false

    var isNeedRoomMutedStatus = true //是否需要判断禁言状态

    override fun init(container: ViewGroup) {
        super.init(container)
        mContext = container.context

        LayoutInflater.from(mContext).inflate(R.layout.fcr_ease_chat_layout, null, false)?.let {
            layout = it
            contentLayout = it.findViewById(R.id.fragment_container)
            contentLayout?.clipToOutline = true
            contentLayout?.elevation = elevation.toFloat()

            hideLayout = it.findViewById(R.id.chat_hide_icon_layout)
            unreadText = it.findViewById(R.id.chat_unread_text)
            hideLayout.visibility = View.GONE

            val param = MarginLayoutParams(MarginLayoutParams.MATCH_PARENT, MarginLayoutParams.MATCH_PARENT)
            container.addView(layout, param)
        }
        Constants.AgoraLog?.e("$tag -> token007 = $token")
        addEaseIM()
    }

    fun setInputViewParent(viewGroup: ViewGroup) {
        this.specialInputViewParent = viewGroup
    }

    private fun getInputViewParent(): ViewGroup? {
        return specialInputViewParent
    }

    private fun addEaseIM() {
        nickName = widgetInfo?.localUserInfo?.userName ?: ""
        userUuid = widgetInfo?.localUserInfo?.userUuid ?: ""
        roomUuid = widgetInfo?.roomInfo?.roomUuid ?: ""

        if (parseEaseConfigProperties()) {
            chatViewPager = mContext?.let { ChatViewPager(it) }
            chatViewPager?.let {
                it.setCloseable(false)
                it.setAvatarUrl(avatarUrl)
                it.setChatRoomId(mChatRoomId)
                it.setNickName(nickName)
                it.setRoomUuid(roomUuid)
                it.setUserName(userName)
                it.setUserUuid(userUuid)
                it.setRoomType(widgetInfo?.roomInfo?.roomType!!)
                token?.let { it1 -> it.setUserToken(it1) }
                it.setBaseUrl(AgoraEduSDK.baseUrl())
                it.setAppId(Constants.APPID)
            }

//            chatViewPager?.isNeedRoomMutedStatus = eduC

            contentLayout?.removeAllViews()
            contentLayout?.addView(chatViewPager)
            chatViewPager?.chatPagerListener = this

            if (appKey.isNotEmpty() && EaseIM.getInstance().init(mContext, appKey)) {
                EaseRepository.instance.isInit = true
                chatViewPager?.fetchIMToken()
            } else {
                mContext?.let {
                    AgoraUIToast.error(
                        context = it.applicationContext, text = mContext?.getString(
                            R.string.fcr_login_chat_failed
                        ) + "--" +
                                mContext?.getString(R.string.fcr_appKey_is_empty)
                    )
                }
            }

            mContext?.let {
                inputView = InputView(it)
                val params = RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                val inputParent = getInputViewParent() ?: container?.parent
                inputView?.let { input ->
                    input.layoutParams = params
                    if (inputParent != null && inputParent is ViewGroup) {
                        inputParent.addView(input)
                        input.visibility = View.INVISIBLE
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
            role = this.widgetInfo?.localUserInfo?.userRole?: EaseConstant.ROLE_STUDENT
            EaseRepository.instance.role = role
        }

        this.widgetInfo?.localUserProperties?.let {
            userName = it[userIdKey].toString()
        }
        return !TextUtils.isEmpty(userName)
                && !TextUtils.isEmpty(orgName)
                && !TextUtils.isEmpty(appName)
                && !TextUtils.isEmpty(mChatRoomId)
                && !TextUtils.isEmpty(appKey)
    }

    override fun onWidgetRoomPropertiesUpdated(
            properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
            keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        if (properties.keys.contains(appNameKey) && properties.keys.contains(chatRoomIdKey) && !initLoginEaseIM) {
            contentLayout?.post { addEaseIM() }
        }
    }

    override fun release() {
        super.release()
        chatViewPager?.logout()
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

    override fun onImageClick(message: ChatMessage) {
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
        if (isMuted && inputView?.visibility == View.VISIBLE) {
            inputView?.editContent?.let { CommonUtil.hideSoftKeyboard(it) }
            inputView?.visibility = View.GONE
        }
    }

    override fun onIconHideenClick() {
        dismiss()
    }

    override fun onShowUnread(show: Boolean) {
        ThreadManager.instance.runOnMainThread {
            if (hideLayout.visibility == View.VISIBLE)
                unreadText.visibility = View.VISIBLE
            else
                unreadText.visibility = if (show) View.VISIBLE else View.GONE
        }
        broadcasterUnreadTip(show)
        // 收到消息一直是 false
        chatWidgetListener?.onShowUnread(show)
    }

    private fun broadcasterUnreadTip(show: Boolean) {
        val body = AgoraChatInteractionPacket(UnreadTips, show)
        sendMessage(Gson().toJson(body))
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

    override fun setClosable(closable: Boolean) {
        chatViewPager?.setCloseable(closable)
    }

    override fun setMuteViewDisplayed(displayed: Boolean) {
        chatViewPager?.setMuteViewVisibility(displayed)
    }

    override fun setChatLayoutBackground(background: Int) {
        chatViewPager?.setChatLayoutBackground(background)
    }

    /**
     * displayed: false,只有聊天
     */
    override fun setTabDisplayed(displayed: Boolean) {
        chatViewPager?.setCloseable(displayed)
        chatViewPager?.setTabLayoutCloseable(displayed)
    }

    /**
     * displayed: false,关闭输入框
     */
    override fun setInputViewDisplayed(displayed: Boolean) {
        chatViewPager?.setInputViewCloseable(displayed)

    }

    override fun setBackground(background: Int) {
        chatViewPager?.setBackgroundResource(background)
    }
}