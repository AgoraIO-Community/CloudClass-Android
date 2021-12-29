package io.agora.agoraeduuikit.impl.whiteboard.bean

import android.graphics.Color
import android.webkit.WebView
import java.util.*

// Board and Style params are used in multi-view modes
data class BoardParams(
        val useMultiViews: Boolean = false,
        val styleParams: BoardStyleParams? = null
)

data class BoardStyleParams(
        val left: Int = 0,
        val bottom: Int = 0,
        val styles: List<String>
)

internal object BoardStyleInjector {
    private var left: Int = 0
    private var bottom: Int = 0
    private var styles: MutableList<String> = mutableListOf()

    fun setPosition(left: Int = 0, bottom: Int = 0) {
        this.left = left
        this.bottom = bottom
    }

    fun getPositionStyle(): HashMap<String, String> {
        val map = HashMap<String, String>()
        map["position"] = "fixed"
        map["left"] = left.toString().plus("px")
        map["bottom"] = bottom.toString().plus("px")
        return map
    }

    fun addStyle(style: String) {
        styles.add(style)
    }

    private const val javascriptFormat =
        "var style = document.createElement('style');\n" +
            "style.innerHTML = %s\n" +
            "document.head.appendChild(style);"

    fun injectBoardStyles(view: WebView) {
        val locale = Locale.getDefault()
        styles.forEach { js ->
            injectBoardStyle(locale, view, js)
        }
    }

    private fun injectBoardStyle(locale: Locale, view: WebView, attr: String) {
        val js = String.format(locale, javascriptFormat, attr)
        view.loadUrl("javascript: $js")
    }
}


data class WhiteboardDrawingConfig(
    var activeAppliance: WhiteboardApplianceType = WhiteboardApplianceType.Clicker,
    var color: Int = Color.WHITE,
    var fontSize: Int = 22,
    var thick: Int = 4) {

    fun set(config: WhiteboardDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}

enum class WhiteboardApplianceType(val value: Int) {
    Select(0),
    Pen(1),
    Rect(2),
    Circle(3),
    Line(4),
    Eraser(5),
    Text(6),
    Clicker(7),
    Laser(8);
}

enum class WhiteboardToolType {
    All, Whiteboard
}

object BoardRegionStr {
    const val cn = "cn-hz"
    const val na = "us-sv"
    const val eu = "gb-lon"
    const val ap = "sg"
}

data class AgoraBoardDrawingMemberState(
    var activeApplianceType: WhiteboardApplianceType? = null,
    var strokeColor: Int? = null,
    var strokeWidth: Int? = null,
    var textSize: Int? = null
)

data class AgoraBoardInteractionPacket(val signal: AgoraBoardInteractionSignal, val body: Any)

enum class AgoraBoardInteractionSignal(val value: Int) {
    // EduBoardRoomPhase
    BoardPhaseChanged(1),

    // AgoraBoardDrawingMemberState
    MemberStateChanged(2),

    // AgoraBoardGrantData
    BoardGrantDataChanged(3),

    // Pair<Int, Int>
    RTCAudioMixingStateChanged(4),

    // AgoraBoardAudioMixingRequestData
    BoardAudioMixingRequest(5)
}

data class AgoraBoardAudioMixingRequestData(
    val type: AgoraBoardAudioMixingRequestType,
    val filepath: String = "",
    val loopback: Boolean = false,
    val replace: Boolean = false,
    val cycle: Int = -1,
    val position: Int = -1)

enum class AgoraBoardAudioMixingRequestType(val value: Int) {
    Start(0),
    Stop(1),
    SetPosition(2);
}

data class AgoraBoardGrantData(val granted: Boolean, val userUuids: MutableList<String>)