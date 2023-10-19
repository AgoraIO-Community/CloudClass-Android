package com.agora.edu.component.common

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * author : felix
 * date : 2022/7/7
 * description : 区域组件
 */
class AgoraEduFrameContainerView : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)
}