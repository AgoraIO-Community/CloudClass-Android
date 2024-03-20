package io.agora.online.tab

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.databinding.FcrOnlineEduTabComponentBinding

/**
 * 为AgoraEduTabGroupComponent.tabLayout自定义的自定义tagView
 * custom tagView for AgoraEduTabGroupComponent.tabLayout
 */
class AgoraEduTabComponent : AbsAgoraEduComponent {
    private val tag = "AgoraEduTabComponent"

    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val binding = FcrOnlineEduTabComponentBinding.inflate(LayoutInflater.from(context),
        this, true)

    /**
     * 显示隐藏默认角标（红点）
     * show/hide default markView(red dot)
     */
    fun showMark(visibility: Int) {
        binding.tabMark.post {
            binding.tabMark.visibility = visibility
        }
    }

    /**
     * 显示隐藏自定义的角标view（比如说显示隐藏未读消息条数时）
     * show/hide custom markView(for example: when the number of unread messages is displayed or hidden)
     * @param marView
     * @param visibility if ${visibility} is not VISIBLE, markView can be none.
     */
    fun showMark(marView: View? = null, visibility: Int) {
        // todo alexander
    }
}