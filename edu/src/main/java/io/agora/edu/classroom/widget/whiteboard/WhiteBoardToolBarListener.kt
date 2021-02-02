package io.agora.edu.classroom.widget.whiteboard

interface WhiteBoardToolBarListener {
    fun onSelector()

    /**color: 颜色
     * thickness: 粗细
     * theme: 箭头、直线、荧光笔、*/
    fun onPencil(color: Int, widthIndex: Int, themeIndex: Int)

    fun onText(color: Int, sizeIndex: Int)

    fun onRectangle(color: Int, widthIndex: Int)

    fun onEllipse(color: Int, widthIndex: Int)

    fun onEraser(widthIndex: Int)
}