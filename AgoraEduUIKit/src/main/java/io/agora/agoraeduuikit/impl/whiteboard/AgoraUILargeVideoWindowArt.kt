package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.agoraeducore.core.context.EduContextUserDetailInfo
import io.agora.agoraeducore.core.context.EduContextVideoMode
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.video.AgoraUILargeVideoGroupArt

class AgoraUILargeVideoWindowArt( //
        context: Context,
        private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
        parent: ViewGroup,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        public var userDetailInfo: EduContextUserDetailInfo?
) : AbsComponent() {
    private val tag = "AgoraUILargeVideoWindowArt"

    private var largeVideoWindow = AgoraUILargeVideoGroupArt(parent.context, eduContext, parent, left.toInt(), top.toInt(), width, height, 0, EduContextVideoMode.Single, userDetailInfo)

    init {

    }

    override fun setRect(rect: Rect) {

    }


    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        largeVideoWindow.setVisibility(visibility, userDetailInfo)
    }

    fun setLocation(rect: Rect) {
        largeVideoWindow.setRect(rect)
    }

}