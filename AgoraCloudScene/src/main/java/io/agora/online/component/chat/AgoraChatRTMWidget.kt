package io.agora.online.component.chat

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.online.R
import io.agora.online.impl.chat.AgoraChatInteractionPacket
import io.agora.online.impl.chat.AgoraChatInteractionSignal
import io.agora.online.impl.chat.ChatPopupWidget
import io.agora.online.impl.chat.rtm.*
import io.agora.online.provider.AgoraUIUserDetailInfo

@Deprecated(message = "please use AgoraEduEaseChatWidget")
class AgoraChatRTMWidget : ChatPopupWidget() {
    private lateinit var parent: ViewGroup
    private var shadow: Int = 0

    private lateinit var layout: RelativeLayout

    private lateinit var closeBtn: AppCompatImageView
    private lateinit var muteBtn: AppCompatImageView

    private lateinit var contentLayout: RelativeLayout
    private lateinit var titleLayout: RelativeLayout
    private lateinit var titleDivider: View
    private lateinit var divider: View
    private lateinit var muteLayout: RelativeLayout
    private lateinit var inputLayout: RelativeLayout
    private lateinit var emptyMessagePlaceHolder: RelativeLayout
    private lateinit var recycler: RecyclerView

    private lateinit var edit: AppCompatEditText
    private lateinit var sendBtn: AppCompatTextView

    private var chatManager: AgoraChatManager? = null

    override fun init(parent: ViewGroup) {
        this.parent = parent
        shadow = parent.context.resources.getDimensionPixelSize(R.dimen.shadow_width)
        layout = LayoutInflater.from(parent.context).inflate(R.layout.fcr_online_chat_layout, null, false) as RelativeLayout
        contentLayout = layout.findViewById(R.id.agora_chat_layout)
        titleLayout = layout.findViewById(R.id.agora_chat_title_layout)
        titleDivider = layout.findViewById(R.id.agora_chat_title_divider)
        divider = layout.findViewById(R.id.agora_chat_divider)
        divider.isVisible = false
        muteLayout = layout.findViewById(R.id.agora_chat_student_mute_layout)
        muteLayout.isVisible = false
        inputLayout = layout.findViewById(R.id.agora_chat_input_layout)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        parent.addView(layout, layoutParams)

        closeBtn = layout.findViewById(R.id.agora_chat_icon_close)
        muteBtn = layout.findViewById(R.id.agora_chat_mute_icon)
        muteBtn.isVisible = widgetInfo?.localUserInfo?.userRole == AgoraEduContextUserRole.Teacher.value

        edit = layout.findViewById(R.id.agora_chat_message_edit)
        edit.setOnKeyListener { view, keyCode, event ->
            return@setOnKeyListener if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            ) {
                onSendClick(view)
                true
            } else false
        }

        sendBtn = layout.findViewById(R.id.agora_chat_send_btn)
        sendBtn.setTextColor(Color.WHITE)
        sendBtn.background = RoundRectButtonStateBg(
            layout.resources.getDimensionPixelSize(R.dimen.agora_message_send_btn_width),
            layout.resources.getDimensionPixelSize(R.dimen.agora_message_send_btn_height),
            layout.resources.getColor(R.color.theme_blue_light),
            layout.resources.getColor(R.color.theme_blue_light),
            layout.resources.getColor(R.color.theme_blue_gray),
            layout.resources.getColor(R.color.theme_blue_gray),
            layout.resources.getColor(R.color.theme_disable),
            layout.resources.getColor(R.color.theme_disable),
            layout.resources.getDimensionPixelSize(R.dimen.stroke_small)
        )

        sendBtn.setOnClickListener { onSendClick(it) }

        closeBtn.setOnClickListener {

        }
        setClosable(false)

        emptyMessagePlaceHolder = layout.findViewById(R.id.agora_chat_no_message_placeholder)
        emptyMessagePlaceHolder.isVisible = true

        recycler = layout.findViewById(R.id.agora_chat_recycler)
        initChatManager()

        (widgetInfo?.extraInfo as? Map<String, Any>)?.let { map ->
            (map["muteChat"] as? Boolean)?.let { muted ->
                setChatMuted(muted)
            }
        }
    }

    private fun onSendClick(view: View) {
        val content = edit.text.toString().trim()
        if (TextUtils.isEmpty(content)) {
            return
        }

        edit.setText("")
        hideSoftInput(edit)
        chatManager?.sendLocalTextMessage(content)
    }

    private fun hideSoftInput(editText: AppCompatEditText) {
        val service = editText.context.getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        service.hideSoftInputFromWindow(
            editText.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun initChatManager() {
        // If the chat widget is correctly created, the
        // room info and app id must be initialized properly
        val roomId = widgetInfo?.roomInfo?.roomUuid ?: ""
        val userId = widgetInfo?.localUserInfo?.userUuid ?: ""
        val userName = widgetInfo?.localUserInfo?.userName ?: ""
        val userRole = widgetInfo?.localUserInfo?.userRole ?: AgoraEduContextUserRole.Student.value

        widgetInfo?.extraInfo?.let { extra ->
            (extra as? Map<String, Any>)?.let { extraMap ->
                (extraMap["appId"] as? String)?.let { appId ->
                    chatManager = AgoraChatManager(
                        appId, roomId, userId, userName, userRole, recycler,
                        object : MessageListListener {
                            override fun onMessageListEmpty(empty: Boolean) {
                                emptyMessagePlaceHolder.isVisible = empty
                            }

                            override fun onNewMessageReceived() {
                                checkIfShowUnreadHint()
                            }
                        })

                    chatManager?.pullChatRecord()
                }
            }
        }
    }

    override fun setFullscreenRect(fullScreen: Boolean, rect: Rect) {
        // No implementation
    }

    override fun setFullDisplayRect(rect: Rect) {
        // No implementation
    }

    override fun show(show: Boolean) {
        // No implementation
    }

    override fun isShowing(): Boolean {
        return true
    }

    override fun getLayout(): ViewGroup {
        return layout
    }

    @UiThread
    override fun setClosable(closable: Boolean) {
        closeBtn.visibility = if (closable) View.VISIBLE else View.GONE
    }

    override fun setBackground(back: Int) {
    }

    override fun setTabDisplayed(displayed: Boolean) {
        titleLayout.isVisible = displayed
        titleDivider.isVisible = displayed
    }

    override fun setMuteViewDisplayed(displayed: Boolean) {
    }

    override fun setChatLayoutBackground(background: Int) {
        contentLayout.setBackgroundResource(background)
    }

    override fun setInputViewDisplayed(displayed: Boolean) {
        inputLayout.visibility = if (displayed) LinearLayout.VISIBLE else LinearLayout.GONE
    }

    override fun setPrivateChatViewDisplayed(user: AgoraUIUserDetailInfo?, displayed: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onMessageReceived(message: String) {
        val gson = Gson()
        val map = gson.fromJson(message, Map::class.java)
        (map as? Map<String, Any>)?.let { data ->
            data["message"]?.let { messageItem ->
                val item = gson.fromJson(gson.toJson(messageItem), RemoteChatItem::class.java)
                if (item.messageId?.isNotBlank() != false) {
                    val chatItem = AgoraChatItem()
                    chatItem.uid = item.uid ?: ""
                    chatItem.name = item.name ?: ""
                    chatItem.role = item.role ?: AgoraChatUserRole.Student.value
                    chatItem.messageId = item.messageId ?: ""
                    chatItem.message = item.message ?: ""
                    chatItem.timestamp = item.timestamp ?: 0
                    chatItem.source = AgoraUIChatSource.Remote

                    chatManager?.addRemoteTextMessage(chatItem)
                    chatManager?.refreshRecyclerView()
                    //checkIfShowUnreadHint()

                    chatWidgetListener?.onShowUnread(true)

                    if (chatManager?.getChatItemCount() ?: 0 > 0) {
                        emptyMessagePlaceHolder.isVisible = false
                    }
                }
            }

            (data["mute"] as? Boolean)?.let { muted ->
                setChatMuted(muted)
            }
        }
    }

    private fun checkIfShowUnreadHint() {
        if (!parent.isVisible || layout.width <= 0 || layout.height <= 0) {
            val packet = AgoraChatInteractionPacket(
                AgoraChatInteractionSignal.UnreadTips, true
            )
            sendMessage(Gson().toJson(packet))
        }
    }

    override fun setChatMuted(muted: Boolean) {
        ContextCompat.getMainExecutor(parent.context).execute {
            muteLayout.isVisible = muted
            edit.setHint(
                if (muted) {
                    R.string.fcr_rtm_im_silenced_placeholder
                } else {
                    R.string.fcr_rtm_im_input_placeholder
                }
            )
            edit.isEnabled = !muted
            sendBtn.isEnabled = !muted
        }
    }

    override fun showShadow(show: Boolean) {
        if (show) {
            contentLayout.setBackgroundResource(R.drawable.agora_shadowed_round_rect_bg)
        } else {
            contentLayout.setBackgroundResource(R.drawable.agora_class_room_round_rect_bg)
        }
    }
}

data class RemoteChatItem(
    val uid: String?,
    val name: String?,
    val role: Int?,
    val messageId: String?,
    val message: String?,
    val timestamp: Long?
)

class RoundRectButtonStateBg(
    width: Int, height: Int,
    colorDefault: Int, strokeColorDefault: Int,
    colorPressed: Int, strokeColorPressed: Int,
    colorDisabled: Int, strokeColorDisabled: Int,
    strokeWidth: Int
) : StateListDrawable() {
    init {
        val default = GradientDrawable()
        default.shape = GradientDrawable.RECTANGLE
        default.setSize(width, height)
        default.cornerRadius = height / 2f
        default.setStroke(strokeWidth, strokeColorDefault)
        default.setColor(colorDefault)

        val pressed = GradientDrawable()
        pressed.shape = GradientDrawable.RECTANGLE
        pressed.setSize(width, height)
        pressed.cornerRadius = height / 2f
        pressed.setStroke(strokeWidth, strokeColorPressed)
        pressed.setColor(colorPressed)

        val disabled = GradientDrawable()
        disabled.shape = GradientDrawable.RECTANGLE
        disabled.setSize(width, height)
        disabled.cornerRadius = height / 2f
        disabled.setStroke(strokeWidth, strokeColorDisabled)
        disabled.setColor(colorDisabled)

        addState(intArrayOf(android.R.attr.state_pressed), pressed)
        addState(intArrayOf(-android.R.attr.state_enabled), disabled)
        addState(intArrayOf(android.R.attr.state_enabled), default)
    }
}