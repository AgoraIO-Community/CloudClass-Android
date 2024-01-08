package io.agora.education.home.adapter

import androidx.recyclerview.widget.RecyclerView

/**
 * author : felix
 * date : 2022/9/13
 * description : pre load
 */
abstract class FcrPreloadAdapter<VH : RecyclerView.ViewHolder> : RecyclerView.Adapter<VH>() {
    // 预加载回调
    var setOnPreloadListener: (() -> Unit)? = null

    // 预加载偏移量
    var preloadItemCount = 2

    // 增加预加载状态标记位
    var isPreloading = false

    // 列表滚动状态
    var scrollState = RecyclerView.SCROLL_STATE_IDLE

    val onScroll = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            // 更新滚动状态
            scrollState = newState
            super.onScrollStateChanged(recyclerView, newState)
        }
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        checkPreload(position)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(onScroll)
    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        recyclerView.removeOnScrollListener(onScroll)
    }

    // 判断是否进行预加载
    private fun checkPreload(position: Int) {
        if (setOnPreloadListener != null
            && position == Math.max(itemCount - 1 - preloadItemCount, 0)// 索引值等于阈值
            && scrollState != RecyclerView.SCROLL_STATE_IDLE // 列表正在滚动
            && !isPreloading // 预加载不在进行中
        ) {
            isPreloading = true // 表示正在执行预加载
            setOnPreloadListener?.invoke()
        }
    }
}
