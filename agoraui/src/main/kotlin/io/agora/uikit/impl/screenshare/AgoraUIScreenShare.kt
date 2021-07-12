package io.agora.uikit.impl.screenshare

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import io.agora.educontext.EduContextScreenShareState
import io.agora.educontext.EduContextPool
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.ScreenShareHandler
import io.agora.uikit.impl.AbsComponent

class AgoraUIScreenShare(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        width: Int,
        height: Int,
        left: Int,
        top: Int,
        shadowWidth: Float) : AbsComponent(), View.OnTouchListener {
    private val tag = "AgoraUIScreenShare"

    private val contentView: View = LayoutInflater.from(context).inflate(R.layout.agora_screen_share_layout, parent, false)
    private val cardView: CardView = contentView.findViewById(R.id.cardView)
    private val screenShareContainer: FrameLayout = contentView.findViewById(R.id.screen_share_container_layout)

    private val screenShareHandler = object : ScreenShareHandler() {
        override fun onScreenShareStateUpdated(state: EduContextScreenShareState, streamUuid: String) {
            updateScreenShareState(state, streamUuid)
        }

        override fun onSelectScreenShare(select: Boolean) {
            super.onSelectScreenShare(select)
        }
    }

    init {
        cardView.cardElevation = shadowWidth
        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_screen_share_view_corner)
        cardView.radius = radius.toFloat()
        var params = cardView.layoutParams as ViewGroup.MarginLayoutParams
        val margin = shadowWidth.toInt()
        params.setMargins(margin, margin, margin, margin)
        cardView.layoutParams = params

        parent.addView(contentView, width, height)
        params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.leftMargin = left
        params.topMargin = top
        contentView.layoutParams = params

        eduContext?.screenShareContext()?.addHandler(screenShareHandler)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun updateScreenShareState(state: EduContextScreenShareState, streamUuid: String) {
        contentView.post {
            val sharing = state == EduContextScreenShareState.Start
            contentView.visibility = if (sharing) VISIBLE else GONE
            eduContext?.screenShareContext()?.setScreenShareState(state)

            if (sharing) {
                cardView.setOnTouchListener(this)
                eduContext?.screenShareContext()?.renderScreenShare(screenShareContainer, streamUuid)
                screenShareContainer.setOnHierarchyChangeListener(object : ViewGroup.OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        child?.let {
                            if (child is SurfaceView) {
                                child.setZOrderMediaOverlay(true)
                            }
                        }
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {

                    }
                })
            } else {
                cardView.setOnTouchListener(null)
                eduContext?.screenShareContext()?.renderScreenShare(null, streamUuid)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return true
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