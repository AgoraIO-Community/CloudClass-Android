package io.agora.edu.classroom.widget.whiteboard

import com.herewhite.sdk.domain.Appliance

enum class PenTheme(val value: Int) {
    Arrow(0),
    StraightLine(1),
    Fluorescent(2),
    Pencil(3)
}

/**颜色不需要转换，直接就是颜色值*/
object WhiteBoardToolAttrs {
    val modes = arrayOf(Appliance.SELECTOR, Appliance.PENCIL, Appliance.TEXT, Appliance.RECTANGLE,
            Appliance.ELLIPSE, Appliance.ERASER)

    val penSizes = arrayOf(3.0, 7.0, 12.0, 20.0)

    val penStyles = arrayOf(PenTheme.Arrow, PenTheme.StraightLine, PenTheme.Fluorescent,
            PenTheme.Pencil)

    val textSizes = arrayOf(22.0, 24.0, 26.0, 30.0, 36.0, 42.0, 60.0, 72.0)

    val rectangleSizes = arrayOf(3.0, 7.0, 12.0, 20.0)

    val ellipseSizes = arrayOf(3.0, 7.0, 12.0, 20.0)

    val eraserSizes = arrayOf(3.0, 7.0, 12.0, 20.0)
}

class ToolModeAttr {
    var modeIndex: Int = 0
    var rgb: Int = -1163718
    var thicknessIndex: Int = 0
    var pencilStyleIndex: Int = 3
    var fontSizeIndex: Int = 0

    constructor()

    constructor(modeIndex: Int, color: Int, thicknessIndex: Int, pencilStyleIndex: Int,
                fontSizeIndex: Int) {
        this.modeIndex = modeIndex
        this.rgb = color
        this.thicknessIndex = thicknessIndex
        this.pencilStyleIndex = pencilStyleIndex
        this.fontSizeIndex = fontSizeIndex
    }
}