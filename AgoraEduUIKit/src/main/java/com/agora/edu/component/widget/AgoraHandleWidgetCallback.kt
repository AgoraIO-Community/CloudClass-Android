package com.agora.edu.component.widget

import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetCallback
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame

/**
 * author : felix
 * date : 2022/4/19
 * description :
 */
open class AgoraHandleWidgetCallback : AgoraWidgetCallback {
    override fun onWidgetUpdateSyncFrame(
        widget: AgoraBaseWidget,
        frame: AgoraWidgetFrame,
        contextCallback: EduContextCallback<Unit>?
    ) {

    }

    override fun onWidgetUpdateRoomProperties(
        widget: AgoraBaseWidget,
        properties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>,
        contextCallback: EduContextCallback<Unit>?
    ) {

    }

    override fun onWidgetDeleteRoomProperties(
        widget: AgoraBaseWidget,
        keys: MutableList<String>,
        cause: MutableMap<String, Any>,
        contextCallback: EduContextCallback<Unit>?
    ) {

    }

    override fun onWidgetUpdateUserProperties(
        widget: AgoraBaseWidget,
        properties: MutableMap<String, Any>,
        cause: MutableMap<String, Any>,
        contextCallback: EduContextCallback<Unit>?
    ) {

    }

    override fun onWidgetDeleteUserProperties(
        widget: AgoraBaseWidget,
        keys: MutableList<String>,
        cause: MutableMap<String, Any>,
        contextCallback: EduContextCallback<Unit>?
    ) {

    }

    override fun onWidgetSendMessage(widget: AgoraBaseWidget, msg: String) {

    }

}