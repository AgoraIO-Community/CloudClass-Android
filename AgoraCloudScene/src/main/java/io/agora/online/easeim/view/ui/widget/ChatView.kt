package io.agora.online.easeim.view.ui.widget

import android.Manifest
import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.online.easeim.constant.EaseConstant
import io.agora.online.easeim.manager.ThreadManager
import io.agora.online.easeim.repositories.EaseRepository
import io.agora.online.easeim.utils.CommonUtil
import io.agora.online.easeim.view.adapter.MessageAdapter
import io.agora.online.easeim.view.`interface`.MessageListItemClickListener
import io.agora.online.easeim.view.`interface`.ViewEventListener
import com.permissionx.guolindev.PermissionX
import io.agora.Error
import io.agora.ValueCallBack
import io.agora.agoraeducore.core.context.AgoraEduContextAudioSourceType
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSourceType
import io.agora.online.R
import io.agora.online.config.FcrUIConfigFactory
import io.agora.online.config.component.FcrAgoraChatUIConfig
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.chat.ChatClient
import io.agora.chat.ChatMessage
import io.agora.chat.ChatRoom
import io.agora.util.EMLog
import io.agora.util.VersionUtils

/**
 * 聊天页
 */
class ChatView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) :
    BaseView(context) {
    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    companion object {
        private const val TAG = "ChatFragment"
    }

    private lateinit var recyclerView: RecyclerView
    var roomType: Int? = null

    private lateinit var adapter: MessageAdapter
    private lateinit var announcementView: LinearLayout
    private lateinit var announcementContent: TextView
    private lateinit var fragmentView: RelativeLayout
    private lateinit var defaultLayout: RelativeLayout
    var tvInputView: RelativeLayout? = null
    private lateinit var tvContent: TextView
    private lateinit var faceIcon: ImageView
    private lateinit var picIcon: ImageView
    private lateinit var muteView: FrameLayout
    private lateinit var muteIcon: AppCompatImageView
    private lateinit var unMuteIcon: AppCompatImageView
    private lateinit var tvPrivateChatReceiver: TextView
    private lateinit var tvPrivateTag: TextView
    private lateinit var cPrivateChatUiBox: RelativeLayout
    private lateinit var ivPrivateChatCloseButton: AppCompatImageView

    private var inputContent = ""

    var viewEventListener: ViewEventListener? = null
    var uiAgoraChatConfig: FcrAgoraChatUIConfig? = null

    override fun setLayout() {
        LayoutInflater.from(context).inflate(R.layout.fcr_online_chat_view, this)
    }

    private fun getAgoraChatUIConfig(roomType: Int): FcrAgoraChatUIConfig {
        return FcrUIConfigFactory.getConfig(roomType).agoraChat
    }

    override fun initView() {
        fragmentView = findViewById(R.id.fragment_view)
        announcementView = findViewById(R.id.announcement_view)
        announcementContent = findViewById(R.id.tv_announcement)
        defaultLayout = findViewById(R.id.default_layout)
        tvInputView = findViewById(R.id.input_view)
        tvContent = findViewById(R.id.tv_content)
        faceIcon = findViewById(R.id.iv_face)

        cPrivateChatUiBox = findViewById(R.id.private_chat_ui_box)
        tvPrivateChatReceiver = findViewById(R.id.tv_receiver)
        tvPrivateTag = findViewById(R.id.tv_private)
        ivPrivateChatCloseButton = findViewById(R.id.btn_private_chat_close)
        picIcon = findViewById(R.id.iv_picture)
        muteView = findViewById(R.id.mute_view)
        muteIcon = findViewById(R.id.iv_mute)
        unMuteIcon = findViewById(R.id.iv_unmute)
        recyclerView = findViewById(R.id.rv_list)
        val layoutManager = LinearLayoutManager(context.applicationContext)
        adapter = MessageAdapter()
        adapter.setHasStableIds(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        if (EaseRepository.instance.isStudentRole()) {
            muteView.visibility = View.GONE
        }
    }

    override fun initListener() {
        super.initListener()
        // memory leak
//        easeRepository.addOperationListener(this)
        adapter.setMessageListItemClickListener(object : MessageListItemClickListener {
            override fun onResendClick(message: ChatMessage): Boolean {
                EMLog.e(TAG, "onResendClick")
                message.setStatus(ChatMessage.Status.CREATE)
                ChatClient.getInstance().chatManager().sendMessage(message)
                refresh()
                return true
            }

            override fun onMessageError(message: ChatMessage, code: Int, error: String?) {
                if (code == Error.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(
                            context,
                            context.getString(R.string.fcr_hyphenate_im_message_incloud_illegal_content),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                EMLog.e(TAG, "onMessageError:$code = $error")
            }

            override fun onItemClick(v: View, message: ChatMessage) {
                if (message.type == ChatMessage.Type.IMAGE) {
                    viewEventListener?.onImageClick(message)
                }
            }

            override fun onPrivateChatViewDisplayed(message: ChatMessage) {
                val from: String = message.from
                val nick = message.getStringAttribute(EaseConstant.NICK_NAME, "")

                val currentPrivateReceiver = AgoraUIUserDetailInfo(from, nick, AgoraEduContextUserRole.Observer, false, 0,
                    whiteBoardGranted = false,
                    isLocal = false,
                    hasAudio = false,
                    hasVideo = false,
                    streamUuid = "",
                    streamName = null,
                    streamType = AgoraEduContextMediaStreamType.None,
                    audioSourceType = AgoraEduContextAudioSourceType.None,
                    videoSourceType = AgoraEduContextVideoSourceType.None,
                    audioSourceState = AgoraEduContextMediaSourceState.Close,
                    videoSourceState = AgoraEduContextMediaSourceState.Close
                )

                // Send it to AgoraEduEaseChatWidget
                viewEventListener?.onPrivateReceiverClick(currentPrivateReceiver)

                ThreadManager.instance.runOnMainThread {
                    showPrivateChatView(currentPrivateReceiver)
                }
            }
        })
        announcementView.setOnClickListener {
            viewEventListener?.onAnnouncementClick()
        }
        tvContent.setOnClickListener(this)
        faceIcon.setOnClickListener(this)
        picIcon.setOnClickListener(this)
        muteView.setOnClickListener(this)
        tvPrivateChatReceiver.setOnClickListener(this)
        ivPrivateChatCloseButton.setOnClickListener(this)
    }

    /**
     * 初始化数据
     */
    fun initData() {
        easeRepository.loadHistoryMessages()
        easeRepository.fetchAnnouncement()
        easeRepository.fetchChatRoomMutedStatus()
    }

    /**
     * 刷新聊天列表
     */
    fun refresh() {
        easeRepository.loadMessages()
    }

    fun announcementChange(announcement: String) {
        if (announcement.isNotEmpty()) {
            announcementView.visibility = VISIBLE
            if ("\n" in announcement)
                announcementContent.text = announcement.split("\n")[0]
            else
                announcementContent.text = announcement
        } else {
            announcementView.visibility = GONE
        }
    }

    override fun loadMessageFinish(messages: List<ChatMessage>) {
        if (messages.isNotEmpty()) {
            defaultLayout.visibility = GONE
            adapter.setData(messages)
            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
        } else {
            defaultLayout.visibility = VISIBLE
        }
    }

    override fun loadHistoryMessageFinish() {
        refresh()
    }

    override fun fetchAnnouncementFinish(announcement: String) {
        announcementChange(announcement)
    }

    override fun fetchChatRoomMutedStatus(isMuted: Boolean) {
        if (EaseRepository.instance.isStudentRole()) {
            if (isMuted)
                showMutedView()
            else
                hideMutedView()
        } else {
            changeMuteView()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_content -> {
                viewEventListener?.onMsgContentClick()
            }

            R.id.iv_face -> {
                viewEventListener?.onFaceIconClick()
            }

            R.id.iv_picture -> {
                if (VersionUtils.isTargetQ(context)) {
                    viewEventListener?.onPicIconClick()
                } else {
                    val act = context as AppCompatActivity
                    PermissionX.init(act)
                        .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .request { allGranted, grantedList, deniedList ->
                            if (allGranted) {
                                viewEventListener?.onPicIconClick()
                            } else {
                                Toast.makeText(context, "No enough permissions", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                }
            }

            R.id.announcement_view -> {
                viewEventListener?.onAnnouncementClick()
            }

            R.id.mute_view -> {
                if (EaseRepository.instance.isInit && EaseRepository.instance.isLogin) {
                    muteView.isClickable = false
                    if (unMuteIcon.visibility == View.VISIBLE) {
                        ChatClient.getInstance().chatroomManager().muteAllMembers(
                            EaseRepository.instance.chatRoomId,
                            object : ValueCallBack<ChatRoom> {
                                override fun onSuccess(value: ChatRoom?) {
                                    ThreadManager.instance.runOnMainThread {
                                        changeMuteIcon()
                                        muteView.isClickable = true
                                        EaseRepository.instance.sendOperationMessage(
                                            EaseConstant.SET_ALL_MUTE,
                                            "",
                                            "",
                                            null
                                        )
                                    }
                                }

                                override fun onError(error: Int, errorMsg: String?) {
                                    ThreadManager.instance.runOnMainThread {
                                        muteView.isClickable = true
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.fcr_hyphenate_im_global_mute_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            })
                    } else {
                        ChatClient.getInstance().chatroomManager().unmuteAllMembers(
                            EaseRepository.instance.chatRoomId,
                            object : ValueCallBack<ChatRoom> {
                                override fun onSuccess(value: ChatRoom?) {
                                    ThreadManager.instance.runOnMainThread {
                                        changeMuteIcon()
                                        muteView.isClickable = true
                                        EaseRepository.instance.sendOperationMessage(
                                            EaseConstant.REMOVE_ALL_MUTE,
                                            "",
                                            "",
                                            null
                                        )
                                    }
                                }

                                override fun onError(error: Int, errorMsg: String?) {
                                    ThreadManager.instance.runOnMainThread {
                                        muteView.isClickable = true
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.fcr_hyphenate_im_global_remove_mute_failed),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            })
                    }
                }
            }

            R.id.tv_receiver -> {
                viewEventListener?.onMsgReceiverClick()
            }

            R.id.btn_private_chat_close -> {
                viewEventListener?.onPrivateReceiverClick(null)
                hidePrivateChatView()
            }
        }
    }

    /**
     * 显示禁言UI
     */
    fun showMutedView() {
        if (EaseRepository.instance.allMuted) {
            tvContent.hint = context.getString(R.string.fcr_hyphenate_im_all_mute)
        } else if (EaseRepository.instance.singleMuted) {
            tvContent.hint = context.getString(R.string.fcr_hyphenate_im_mute)
        }
        tvContent.isClickable = false
        faceIcon.isClickable = false
        picIcon.isClickable = false
    }

    /**
     * 隐藏禁言UI
     */
    fun hideMutedView() {
        tvContent.hint = context.getString(R.string.fcr_hyphenate_im_enter_contents)
        if (inputContent.isNotEmpty()) {
            tvContent.hint = inputContent
        }
        tvContent.isClickable = true
        faceIcon.isClickable = true
        picIcon.isClickable = true
    }

    private var mInitialYOfPrivateChatUiBox // Only for showPrivateChatView
            = 0f
    private var mHeightOfPrivateChatUiBox = 0f

    private fun doFadeInAnimationForPrivateChatUiBox() {
        // With fade-in animation
        cPrivateChatUiBox.visibility = VISIBLE
        cPrivateChatUiBox.alpha = 0.0f
        cPrivateChatUiBox.y = mInitialYOfPrivateChatUiBox
        val animation = cPrivateChatUiBox.animate()
        animation.translationY(-mHeightOfPrivateChatUiBox).alpha(1.0f).setListener(
            object : Animator.AnimatorListener {
                override fun onAnimationStart(animator: Animator) {
                }

                override fun onAnimationCancel(animator: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationEnd(animator: Animator) {
                    animation.setListener(null)
                }
            }
        ).start()
    }

    fun showPrivateChatView(user: AgoraUIUserDetailInfo?) {
        if(user==null){ // All
            tvPrivateChatReceiver.text = CommonUtil.CHAT_ALL_USER_NAME
            tvPrivateChatReceiver.tag = CommonUtil.CHAT_ALL_USER_ID
            tvPrivateTag.visibility = View.GONE
        }else {
            tvPrivateChatReceiver.text = user.userName
            tvPrivateChatReceiver.tag = user.userUuid
            tvPrivateTag.visibility = View.VISIBLE
        }

//        if (cPrivateChatUiBox.visibility == VISIBLE) {
//            // Do not need to repeat the fade-in animation when already visible
//            return
//        }

//        cPrivateChatUiBox.visibility = INVISIBLE // Trigger the layoutting
//        cPrivateChatUiBox.requestLayout()
//
//        val avoidCallingTwice = mHeightOfPrivateChatUiBox;
//
//        // W/o fade-in animation, you will not see the box, because it's under the input box
//        cPrivateChatUiBox.viewTreeObserver.addOnGlobalLayoutListener {
//            if (mInitialYOfPrivateChatUiBox == 0f && mHeightOfPrivateChatUiBox >= 0F) {
//                mInitialYOfPrivateChatUiBox = cPrivateChatUiBox.y
//                mHeightOfPrivateChatUiBox = cPrivateChatUiBox?.height?.toFloat()!!
//                doFadeInAnimationForPrivateChatUiBox()
//            }
//        }
//
//        if (avoidCallingTwice > 0f) {
//            doFadeInAnimationForPrivateChatUiBox()
//        }
    }

    fun hidePrivateChatView() {
        tvPrivateChatReceiver.text = null
        tvPrivateChatReceiver.tag = null
        cPrivateChatUiBox.visibility = INVISIBLE
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) {
            handler.postDelayed({
                refresh()
            }, 300)

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        easeRepository.addOperationListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        easeRepository.removeOperationListener(this)
    }

    fun setInputContent(content: String) {
        inputContent = content
        if (!EaseRepository.instance.singleMuted || EaseRepository.instance.allMuted) {
            hideMutedView()
        }
    }

    private fun changeMuteIcon() {
        if (unMuteIcon.visibility == View.VISIBLE) {
            unMuteIcon.visibility = View.GONE
            muteIcon.visibility = View.VISIBLE
        } else {
            unMuteIcon.visibility = View.VISIBLE
            muteIcon.visibility = View.GONE
        }
    }

    private fun changeMuteView() {
        if (EaseRepository.instance.allMuted) {
            unMuteIcon.visibility = View.GONE
            muteIcon.visibility = View.VISIBLE
        } else {
            unMuteIcon.visibility = View.VISIBLE
            muteIcon.visibility = View.GONE
        }
    }

    fun setMuteViewVisibility(visibility: Boolean) {
        if (visibility) {
            muteView.visibility = VISIBLE
        } else {
            muteView.visibility = GONE
        }
    }

    fun setRoomType(roomType: Int) {
        this.roomType = roomType
        this.uiAgoraChatConfig = roomType?.let { getAgoraChatUIConfig(it) }

        if (uiAgoraChatConfig?.emoji?.isVisible != true) {
            faceIcon.visibility = GONE
        }
        if (uiAgoraChatConfig?.picture?.isVisible != true) {
            picIcon.visibility = GONE
        }
        if (uiAgoraChatConfig?.muteAll?.isVisible != true) {
            muteView.visibility = GONE
        }
    }


}

enum class RoomType(var value: Int) {
    ONE_ON_ONE(0),

    // The old version of medium class
    SMALL_CLASS(4),

    LARGE_CLASS(2),

    /**
     * 分组小班课
     */
    GROUPING_CLASS(101);

    companion object {
        val map = hashMapOf(
            ONE_ON_ONE.value to ONE_ON_ONE,
            SMALL_CLASS.value to SMALL_CLASS,
            LARGE_CLASS.value to LARGE_CLASS,
            GROUPING_CLASS.value to GROUPING_CLASS,
        )

        fun roomTypeIsValid(value: Int): Boolean {
            return map[value] != null
        }

        fun getRoomType(value: Int): RoomType? {
            return map[value]
        }

        fun fromValue(value: Int): RoomType {
            return when (value) {
                ONE_ON_ONE.value -> ONE_ON_ONE
                SMALL_CLASS.value -> SMALL_CLASS
                LARGE_CLASS.value -> LARGE_CLASS
                GROUPING_CLASS.value -> GROUPING_CLASS
                else -> ONE_ON_ONE
            }
        }
    }
}