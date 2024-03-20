package io.agora.online.component.teachaids.vote

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * author : cjw
 * date : 2022/3/25
 * description :
 * 投票器布局管理器，主要用来监听布局完成的回调
 * Vote LayoutManager，listen layout complete callback
 */
class VoteLinearLayoutManager(context: Context, val layoutListener: OnLayoutListener? = null) : LinearLayoutManager(context) {
    private val tag = "MyLinearLayoutManager"

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        Log.i(tag, "->parentWidth onLayoutCompleted")
        layoutListener?.onLayoutCompleted()
    }

    interface OnLayoutListener {
        fun onLayoutCompleted()
    }
}