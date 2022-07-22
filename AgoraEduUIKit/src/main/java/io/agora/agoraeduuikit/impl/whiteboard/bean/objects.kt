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
    var fontSize: Int = 36,
    var thick: Int = 2) {

    fun set(config: WhiteboardDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}

/**
 * 分段方便分类，和业务一致
 */
enum class WhiteboardApplianceType(val value: Int) {
    // 101-200
    Rect(101),
    PenS(102),         // 画笔-S = Pen
    Line(103),
    Circle(104),
    Laser(105),      // 激光笔 TODO
    Star(106),       // 五角星
    Rhombus(107),    // 菱形
    Arrow(108),      // 箭头
    Triangle(109),   // 三角形

    // 1-100
    Clicker(1),     // 拖动
    Select(2),      // 选择框
    Pen(3),         // 画笔
    Text(4),        // 文本
    Eraser(5),      // 橡皮擦
    WB_Clear(6),      // 白板 - 清空
    WB_Pre(7),        // 白板 - 上一个步骤
    WB_Next(8),       // 白板 - 下一个步骤

    // 201-300
    Tool_Cloud(201),      // 云盘
    Tool_CountDown(202),  // 倒计时
    Tool_Selector(203),   // 答题器
    Tool_Polling(204),   // 答题器
    Tool_WhiteBoard_Switch(205);    // 白板开关

    fun isPen(): Boolean {
        return value in 101..200 && value!=105
    }
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
    BoardAudioMixingRequest(5),

    // Load Courseware
    LoadCourseware(6),

    //打开白板
    BoardOpened(7),

    //关闭白板
    BoardClosed(8),
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