package com.agora.edu.component.teachaids

/**
 * author : cjw
 * date : 2022/2/23
 * description :
 */

object TeachAidStatics {
    const val EXTRA_KEY_APPID = "appId"
}

data class AgoraTeachAidWidgetInteractionPacket(val signal: AgoraTeachAidWidgetInteractionSignal, val body: Any)

enum class AgoraTeachAidWidgetInteractionSignal(val value: Int) {
    // send this message when one widget needs to make itself inactive or needs to be created.
    // and an external listener calls the setInActive/setActive/create method of the widgetContext for the widget
    // true: Active   false: InActive
    ActiveState(0),
    // if widget position or size has change, you can send this signal to parent, parent will relayout widget follow
    // its width/height
    NeedRelayout(1);
}

data class AgoraTeachAidWidgetActiveStateChangeData(
    val active: Boolean,
    // extraProperties
    val properties: Map<String, Any>? = null
)