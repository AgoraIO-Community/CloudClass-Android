package io.agora.uikit.impl.users

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.uikit.R
import io.agora.uikit.component.RatioRelativeLayout
import io.agora.uikit.component.SquareRelativeLayout
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.impl.container.AgoraUIConfig
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
                               private val itemShadowWidth: Float
) : AbsComponent() {
    private val tag = "AgoraUserListVideoLayout"
    private lateinit var mVideoAdapter: CoHostVideoAdapter
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_userlist_video_layout, parent, false)
    private val volumeUpdateRun = VolumeUpdateRun()
    private var margin: Int = 0
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)

    private val userHandler = object : UserHandler() {
        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            super.onVolumeUpdated(volume, streamUuid)
            updateAudioVolumeIndication(volume, streamUuid)
        }
    }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            mVideoAdapter.notifyItemInserted(position)
        }

        override fun onRemoved(position: Int, count: Int) {
            mVideoAdapter.notifyItemRemoved(position)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            mVideoAdapter.notifyItemMoved(fromPosition, toPosition)
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
        margin = parent.context.resources.getDimensionPixelSize(R.dimen.margin_small)
        parent.addView(layout, width, height)

        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
        params.width = width
        params.height = height
        params.topMargin = top
        params.leftMargin = left
        layout.layoutParams = params

        val leftArrow = layout.findViewById<ImageView>(R.id.iv_arrow_left)
        val rightArrow = layout.findViewById<ImageView>(R.id.iv_arrow_right)

        // remove the animator when refresh item
        recyclerView?.itemAnimator?.addDuration = 0
        recyclerView?.itemAnimator?.changeDuration = 0
        recyclerView?.itemAnimator?.moveDuration = 0
        recyclerView?.itemAnimator?.removeDuration = 0
        (recyclerView?.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        bindArrowWithRv(leftArrow, rightArrow, recyclerView)
        adjustRvItemSize(recyclerView)
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
                val height = parent.height
                val lp = view.layoutParams
                lp.width = height
                view.layoutParams = lp
                super.getItemOffsets(outRect, view, parent, state)
            }
        })
    }

    private fun bindArrowWithRv(leftArrow: ImageView, rightArrow: ImageView, rv: RecyclerView) {
        val updateRun = Runnable {
            leftArrow.isVisible = getRvLeftDistance(rv) > 0
            rightArrow.isVisible = getRvRightDistance(rv) > 0
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
        }
        rv.layoutManager = lm

        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View,
                                        parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                outRect.right = margin

                if (mVideoAdapter.itemCount > 0 && position == mVideoAdapter.itemCount - 1) {
                    outRect.right = 0
                }
            }
        })

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
        val itemCount = layoutManager.itemCount
        val recycleViewWidth: Int = rv.width
        val itemWidth = lastVisibleItem.width
        val lastItemRight = layoutManager.getDecoratedRight(lastVisibleItem)
        return (itemCount - lastItemPosition - 1) * itemWidth - recycleViewWidth + lastItemRight
    }

    private fun getRvLeftDistance(rv: RecyclerView): Int {
        if (rv.childCount == 0) {
            return 0
        }
        val layoutManager = rv.layoutManager as LinearLayoutManager
        val firstVisibleItem: View = rv.getChildAt(0)
        val firstItemPosition = layoutManager.findFirstVisibleItemPosition()
        val itemWidth = firstVisibleItem.width
        val firstItemLeft = layoutManager.getDecoratedLeft(firstVisibleItem)
        return firstItemPosition * itemWidth - firstItemLeft
    }

    fun updateCoHostList(list: MutableList<EduContextUserDetailInfo>) {
        differ.submitList(list.map { VideoItem(it, 0) } as MutableList<VideoItem>)
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        layout.removeCallbacks(volumeUpdateRun)
        volumeUpdateRun.value = value
        volumeUpdateRun.streamUuid = streamUuid
        layout.post(volumeUpdateRun)
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

    private inner class CoHostVideoAdapter(val shadowWidth: Float,
                                           val callback: IAgoraUIUserListListener?): RecyclerView.Adapter<VideoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            val container = if (AgoraUIConfig.isLargeScreen) {
                val layout = RatioRelativeLayout(parent.context)
                layout.setRatio(16 / 9f)
                layout
            } else {
                SquareRelativeLayout(parent.context)
            }

            return VideoHolder(container, AgoraUIVideo(parent.context,
                    container, 0f, 0f, shadowWidth), callback)
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