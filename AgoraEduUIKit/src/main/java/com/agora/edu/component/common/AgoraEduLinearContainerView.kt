package com.agora.edu.component.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.LinearLayoutCompat

/**
 * author : felix
 * date : 2022/7/7
 * description : 区域组件
 */
class AgoraEduLinearContainerView : LinearLayoutCompat {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    init {
        this.orientation = LinearLayoutCompat.VERTICAL
    }
}