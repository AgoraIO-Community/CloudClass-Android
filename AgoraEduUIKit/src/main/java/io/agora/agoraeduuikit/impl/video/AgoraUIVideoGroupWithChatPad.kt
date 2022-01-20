package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.context.EduContextVideoMode
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeduuikit.provider.UIDataProvider
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.chat.ChatPopupWidget
import io.agora.agoraeduuikit.impl.chat.ChatWidget
import io.agora.agoraeduuikit.impl.chat.EaseChatWidgetPopup


class AgoraUIVideoGroupWithChatPad(
    context: Context,
    private val eduContext: EduContextPool?,
    private val parent: ViewGroup,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
    private val margin: Int,
    mode: EduContextVideoMode = EduContextVideoMode.Single,
    uiDataProvider: UIDataProvider?) : AbsComponent() {

    private val tag = "AgoraUIVideoGroup"

    private val contentView = LinearLayout(context)
    private val videoLayout = LinearLayout(context)
    private val chatLayout = LinearLayout(context)
    private var teacherVideoWindow: AgoraUIVideoGroup? = null
    private var chat: ChatWidget? = null

    private val roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            eduContext?.widgetContext()?.getWidgetConfig(AgoraWidgetDefaultId.Chat.id)?.let { config ->
                (eduContext.widgetContext()?.create(config) as? ChatPopupWidget)?.let { popup ->
                    (contentView as? ViewGroup)?.let { container ->
                        container.post {
                            (popup as? EaseChatWidgetPopup)?.setInputViewParent(parent)
                            popup.init(chatLayout, chatLayout.width, chatLayout.height, 0, 0)
                            popup.setClosable(false)
                            popup.setTabDisplayed(false)
                        }
                    }
                    chat = popup
                }
            }
        }
    }

    init {
        initLayoutView(width, height, left, top)

        val viewHeight = height / 2
        contentView.addView(videoLayout, width, viewHeight)
        contentView.addView(chatLayout, width, viewHeight)
        contentView.setBackgroundResource(R.color.agora_board_preload_progress_view_progressbar_bg)

        initVideoLayout(width, viewHeight)

        eduContext?.roomContext()?.addHandler(roomHandler)
        teacherVideoWindow?.let {
            uiDataProvider?.addListener(it.uiDataProviderListener)
        }
    }

    private fun initLayoutView(width: Int, height: Int, left: Int, top: Int) {
        parent.addView(contentView, width, height)

        (contentView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { params ->
            params.leftMargin = left
            params.topMargin = top
            contentView.layoutParams = params
        }

        contentView.orientation = LinearLayout.VERTICAL
        contentView.setBackgroundResource(
            R.color.agora_board_preload_progress_view_progressbar_bg)
    }

    private fun initVideoLayout(width: Int, height: Int) {
        teacherVideoWindow = AgoraUIVideoGroup(parent.context, eduContext,
            videoLayout, 0, 0, width,
            height, margin, EduContextVideoMode.Pair)
    }

    fun show(show: Boolean) {
        videoLayout.post {
            videoLayout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }
}