package io.agora.edu.classroom.widget.whiteboard

enum class PenTheme(val value: Int) {
    Arrow(0),
    StraightLine(1),
    Fluorescent(2),
    Pencil(3)
}

/**颜色不需要转换，直接就是颜色值*/
object WhiteBoardToolAttrs {
    val penSizes = arrayOf(3.0, 7.0, 12.0, 20.0)
    val penThemes = arrayOf(PenTheme.Arrow, PenTheme.StraightLine, PenTheme.Fluorescent,
                                    PenTheme.Pencil)

    val textSizes = arrayOf(22.0, 24.0, 26.0, 30.0, 36.0, 42.0, 60.0, 72.0)

    val rectangleSizes = arrayOf(3.0, 7.0, 12.0, 20.0)

    val ellipseSizes = arrayOf(3.0, 7.0, 12.0, 20.0)

    val eraserSizes = arrayOf(3.0, 7.0, 12.0, 20.0)
}