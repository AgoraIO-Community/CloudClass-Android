package io.agora.uikit.impl.users

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.uikit.R
import io.agora.uikit.component.adapteranimator.FadeInDownAnimator
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.impl.video.AgoraUIVideo
import io.agora.uikit.interfaces.listeners.IAgoraUIUserListListener
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoListener

class AgoraUserListVideoLayout(context: Context,
                               private val eduContext: EduContextPool?,
                               private val parent: ViewGroup,
                               private val width: Int,
                               private val height: Int,
                               private val left: Int,
                               private val top: Int,
                               private val itemShadowWidth: Float,
                               private val itemMargin: Int
) : AbsComponent() {
    private val tag = "AgoraUserListVideoLayout"
    private lateinit var mVideoAdapter: CoHostVideoAdapter
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_userlist_video_layout, parent, false)
    private val volumeUpdateRun = VolumeUpdateRun()
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)

    private val userHandler = object : UserHandler() {
        override fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onCoHostListUpdated(list)
            recyclerView.post { updateCoHostList(list) }
        }

        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            super.onVolumeUpdated(volume, streamUuid)
            updateAudioVolumeIndication(volume, streamUuid)
        }
    }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemRangeInserted(position, count)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemRangeRemoved(position, count)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemMoved(fromPosition, toPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (recyclerView.findViewHolderForAdapterPosition(position) as? VideoHolder)?.let { holder ->
                payload?.let { payload ->
                    if (payload is Pair<*, *>) {
                        (payload.second as? VideoItem)?.let { item ->
                            holder.bind(item)
                        }
                    }
                }
            }
        }
    }

    private val videoItemMatcher = VideoListItemMatcher()

    private val differ = AsyncListDiffer<VideoItem>(
            listUpdateCallback, AsyncDifferConfig.Builder<VideoItem>(videoItemMatcher).build())

    init {
        initView()
        eduContext?.userContext()?.addHandler(userHandler)
    }

    private fun initView() {
        parent.addView(layout, width, height)

        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
        params.width = width
        params.height = height
        params.topMargin = top
        params.leftMargin = left
        layout.layoutParams = params

        val leftArrow = layout.findViewById<ImageView>(R.id.iv_arrow_left)
        val rightArrow = layout.findViewById<ImageView>(R.id.iv_arrow_right)

        bindArrowWithRv(leftArrow, rightArrow, recyclerView)
        adjustRvItemSize(recyclerView)
        adjustRvItemAnimator(recyclerView)
        initRvAdapter(recyclerView)
    }

    private fun initRvAdapter(rv: RecyclerView) {
        mVideoAdapter = CoHostVideoAdapter(itemShadowWidth, object : IAgoraUIUserListListener {
            override fun onMuteVideo(mute: Boolean) {

            }

            override fun onMuteAudio(mute: Boolean) {

            }

            override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
                eduContext?.userContext()?.renderVideo(viewGroup, streamUuid)
            }
        })

        rv.adapter = mVideoAdapter
    }

    fun show(show: Boolean) {
        layout.post {
            layout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    fun isShown(): Boolean {
        return layout.visibility == View.VISIBLE
    }

    private fun adjustRvItemSize(rv: RecyclerView) {
        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)

                val param = view.layoutParams as ViewGroup.MarginLayoutParams
                param.width = calculateVideoWidth()
                param.height = height
                view.layoutParams = param

                // this callback is invoked for only once per
                // view add/remove, but here we must insure every
                // time that all item margins are adjusted correctly
                // when dynamically insert/remove child items.
                calculateItemMargins(parent)
            }
        })
    }

    private fun calculateItemMargins(recyclerView: RecyclerView) {
        recyclerView.forEach { child ->
            val param = child.layoutParams as ViewGroup.MarginLayoutParams
            val position = recyclerView.getChildAdapterPosition(child)
            param.rightMargin = if (position == recyclerView.adapter?.itemCount ?: 0 - 1) 0 else itemMargin
            child.layoutParams = param
        }
    }

    private fun adjustRvItemAnimator(rv: RecyclerView) {
        rv.itemAnimator = FadeInDownAnimator()
        rv.itemAnimator?.addDuration = 600
        rv.itemAnimator?.removeDuration = 600
        rv.itemAnimator?.moveDuration = 600
        rv.itemAnimator?.changeDuration = 600
    }

    private fun bindArrowWithRv(leftArrow: ImageView, rightArrow: ImageView, rv: RecyclerView) {
        val updateRun = Runnable {
            val videoW = calculateVideoWidth()
            leftArrow.isVisible = getRvLeftDistance(rv) > videoW
            rightArrow.isVisible = getRvRightDistance(rv) > videoW
        }

        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    rv.removeCallbacks(updateRun)
                    rv.post(updateRun)
                }
            }
        })

        val lm = object : LinearLayoutManager(layout.context, HORIZONTAL, false) {
            override fun onLayoutCompleted(state: RecyclerView.State?) {
                super.onLayoutCompleted(state)
                rv.removeCallbacks(updateRun)
                rv.post(updateRun)
            }

            override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        rv.layoutManager = lm

        leftArrow.layoutParams = leftArrow.layoutParams.apply {
            val param = this as ViewGroup.MarginLayoutParams
            param.topMargin = itemShadowWidth.toInt()
            param.bottomMargin = itemShadowWidth.toInt()
        }

        rightArrow.layoutParams = rightArrow.layoutParams.apply {
            val param = this as ViewGroup.MarginLayoutParams
            param.topMargin = itemShadowWidth.toInt()
            param.bottomMargin = itemShadowWidth.toInt()
        }
    }

    private fun getRvRightDistance(rv: RecyclerView): Int {
        if (rv.childCount == 0) return 0
        val layoutManager = rv.layoutManager as LinearLayoutManager
        val lastVisibleItem: View = rv.getChildAt(rv.childCount - 1)
        val lastItemPosition = layoutManager.findLastVisibleItemPosition()
        if (lastItemPosition < 0) {
            return 0
        }
        val itemCount = layoutManager.itemCount
        val recycleViewWidth: Int = rv.width
        val itemWidth = lastVisibleItem.width
        val lastItemRight = layoutManager.getDecoratedRight(lastVisibleItem)
        return (itemCount - lastItemPosition - 1) * itemWidth - recycleViewWidth + lastItemRight
    }

    private fun getRvLeftDistance(rv: RecyclerView): Int {
        if (rv.childCount == 0) return 0
        val layoutManager = rv.layoutManager as LinearLayoutManager
        val firstVisibleItem: View = rv.getChildAt(0)
        val firstItemPosition = layoutManager.findFirstVisibleItemPosition()
        if (firstItemPosition < 0) {
            return 0
        }
        val itemWidth = firstVisibleItem.width
        val firstItemLeft = layoutManager.getDecoratedLeft(firstVisibleItem)
        return firstItemPosition * itemWidth - firstItemLeft
    }

    private fun updateCoHostList(list: MutableList<EduContextUserDetailInfo>) {
        if (list.size > 0) {
            differ.submitList(list.map { VideoItem(it, 0) } as MutableList<VideoItem>)
        } else {
            differ.submitList(null)
        }
    }

    private fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        layout.removeCallbacks(volumeUpdateRun)
        volumeUpdateRun.value = value
        volumeUpdateRun.streamUuid = streamUuid
        recyclerView.post(volumeUpdateRun)
    }

    override fun setRect(rect: Rect) {
        // do nothing
    }

    inner class VolumeUpdateRun : Runnable {
        var value: Int = 0
        var streamUuid: String = ""

        override fun run() {
            if (TextUtils.isEmpty(streamUuid)) {
                return
            }
            var index = 0
            for (item in differ.currentList) {
                if (item.info.streamUuid == streamUuid) {
                    item.audioVolume = value
                    val curHolder = recyclerView.findViewHolderForAdapterPosition(index)
                    if (curHolder != null && curHolder is VideoHolder) {
                        curHolder.updateAudioVolumeIndication(value, streamUuid)
                    }
                    break
                }
                index++
            }
        }
    }

    private fun calculateVideoWidth(): Int {
        return (this.height * 16f / 9).toInt()
    }

    private inner class CoHostVideoAdapter(val shadowWidth: Float,
                                           val callback: IAgoraUIUserListListener?) : RecyclerView.Adapter<VideoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            val layout = RelativeLayout(parent.context)
            return VideoHolder(layout, AgoraUIVideo(parent.context,
                layout, 0f, 0f, shadowWidth), callback)
        }

        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            holder.bind(differ.currentList[holder.adapterPosition])
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }
}

internal data class VideoItem(
        var info: EduContextUserDetailInfo,
        var audioVolume: Int
)

internal class VideoListItemMatcher : DiffUtil.ItemCallback<VideoItem>() {
    override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return (oldItem.info.user.userUuid == newItem.info.user.userUuid)
    }

    override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return (oldItem.info.user.userName == newItem.info.user.userName
                && oldItem.info.onLine == newItem.info.onLine
                && oldItem.info.coHost == newItem.info.coHost
                && oldItem.info.boardGranted == newItem.info.boardGranted
                && oldItem.info.cameraState == newItem.info.cameraState
                && oldItem.info.microState == newItem.info.microState
                && oldItem.info.enableAudio == newItem.info.enableAudio
                && oldItem.info.enableVideo == newItem.info.enableVideo
                && oldItem.info.silence == newItem.info.silence
                && oldItem.info.rewardCount == newItem.info.rewardCount
                && oldItem.audioVolume == newItem.audioVolume)
    }

    override fun getChangePayload(oldItem: VideoItem, newItem: VideoItem): Any {
        return Pair(oldItem, newItem)
    }
}

internal class VideoHolder(view: View,
                           val videoUi: AgoraUIVideo,
                           val callback: IAgoraUIUserListListener?
) : RecyclerView.ViewHolder(view), IAgoraUIVideoListener {
    fun bind(item: VideoItem) {
        videoUi.videoListener = this
        videoUi.upsertUserDetailInfo(item.info)
        videoUi.updateAudioVolumeIndication(item.audioVolume, item.info.streamUuid)
    }

    fun updateAudioVolumeIndication(volume: Int, streamUuid: String) {
        videoUi.updateAudioVolumeIndication(volume, streamUuid)
    }

    override fun onUpdateVideo(enable: Boolean) {
        callback?.onMuteVideo(!enable)
    }

    override fun onUpdateAudio(enable: Boolean) {
        callback?.onMuteAudio(!enable)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        callback?.onRendererContainer(viewGroup, streamUuid)
    }
}

