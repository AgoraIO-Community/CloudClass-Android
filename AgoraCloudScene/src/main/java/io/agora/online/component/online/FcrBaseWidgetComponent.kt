package io.agora.online.component.online

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.view.FcrDragTouchGroupView
import io.agora.online.widget.FcrWidgetInfoListener
import io.agora.agoraeducore.core.context.EduContextRoomInfo
import io.agora.agoraeducore.core.internal.framework.impl.handler.RoomHandler
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetActiveObserver
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget

/**
 * author : felix
 * date : 2023/6/20
 * description :
 */
abstract class FcrBaseWidgetComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var widgetListener: FcrWidgetInfoListener? = null

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)

        eduContext?.roomContext()?.addHandler(roomHandler)

        getRegisterWidgetIds().forEach {
            eduContext?.widgetContext()?.addWidgetActiveObserver(widgetObserver, it)
        }
    }

    fun setWidgetInfoListener(listener: FcrWidgetInfoListener) {
        this.widgetListener = listener
    }

    abstract fun getRegisterWidgetIds(): List<String>

    abstract fun createWidget(widgetId: String)

    val widgetObserver = object : AgoraWidgetActiveObserver {
        override fun onWidgetActive(widgetId: String) {
            ContextCompat.getMainExecutor(context).execute {
                createWidget(widgetId)
            }
        }

        override fun onWidgetInActive(widgetId: String) {
            ContextCompat.getMainExecutor(context).execute {
                removeWidget(widgetId)
            }
        }
    }

    open fun removeWidget(widgetId: String) {
        widgetsMap.remove(widgetId)?.release()
    }

    fun createWidget(widgetId: String, container: ViewGroup): AgoraBaseWidget? {
        if (widgetsMap.containsKey(widgetId)) {
            widgetsMap[widgetId]?.release()
        }
        val widgetConfig = eduContext?.widgetContext()?.getWidgetConfig(widgetId)
        widgetConfig?.let {
            val widget = eduContext?.widgetContext()?.create(widgetConfig)
            widget?.init(container)
            widgetsMap.put(widgetId, widget!!)
            return widget
        }
        return null
    }


    override fun release() {
        super.release()
        eduContext?.roomContext()?.removeHandler(roomHandler)
        getRegisterWidgetIds().forEach {
            eduContext?.widgetContext()?.removeWidgetActiveObserver(widgetObserver, it)
        }
        widgetsMap.forEach {
            it.value.release()
        }
        widgetsMap.clear()
    }

    open var roomHandler = object : RoomHandler() {
        override fun onJoinRoomSuccess(roomInfo: EduContextRoomInfo) {
            super.onJoinRoomSuccess(roomInfo)
            ContextCompat.getMainExecutor(context).execute {
                getRegisterWidgetIds().forEach {
                    if (eduContext?.widgetContext()?.getWidgetActive(it) == true) {
                        createWidget(it)
                    }
                }
            }
        }
    }

    val onDragClickListener = object : FcrDragTouchGroupView.OnTouchListener {
        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event?.action == MotionEvent.ACTION_DOWN) {
                onClickListener?.onClick(null)
            }
            return true
        }
    }

    private var onClickListener: OnClickListener? = null

    fun setDragOnClickListener(l: OnClickListener?) {
        this.onClickListener = l
    }
}