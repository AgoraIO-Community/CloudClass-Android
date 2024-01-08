package io.agora.online.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.agora.online.component.common.IAgoraUIProvider

/**
 * author : felix
 * date : 2023/6/19
 * description :
 */
class FcrDragGroupView : FcrDragTouchGroupView {
    private var agoraLargeWindowContainer: View? = null //当前view所在的container

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        agoraLargeWindowContainer = agoraUIProvider.getLargeVideoArea()
        setDragRange(agoraLargeWindowContainer?.width ?: 0, agoraLargeWindowContainer?.height ?: 0)
        agoraLargeWindowContainer?.post {
            setDragRange(agoraLargeWindowContainer?.width ?: 0, agoraLargeWindowContainer?.height ?: 0)
        }
        setEnableDrag(true)
    }
}