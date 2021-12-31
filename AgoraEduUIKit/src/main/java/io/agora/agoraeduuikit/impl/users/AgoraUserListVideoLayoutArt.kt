package io.agora.agoraeduuikit.impl.users

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
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.framework.impl.handler.MediaHandler3
import io.agora.agoraeducore.core.internal.framework.impl.handler.UserHandler
import io.agora.agoraeduuikit.component.RatioRelativeLayout
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoArt
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIUserListListener
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.component.adapteranimator.FadeInDownAnimator

class AgoraUserListVideoLayoutArt(context: Context,
                                  private val eduContext: io.agora.agoraeducore.core.context.EduContextPool?,
                                  private val parent: ViewGroup,
                                  private val width: Int,
                                  private val height: Int,
                                  private val left: Int,
                                  private val top: Int,
                                  private val itemShadowWidth: Float,
                                  private val itemMargin: Int
) : AbsComponent() {
    private val tag = "AgoraUserListVideoLayout"
    private lateinit var mVideoAdapter: CoHostVideoAdapterArt
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_userlist_video_layout, parent, false)
    private val volumeUpdateRun = VolumeUpdateRun()
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
//    private val userHandler = object : UserHandler() {
//        override fun onCoHostListUpdated(list: MutableList<EduContextUserDetailInfo>) {
//            super.onCoHostListUpdated(list)
//            recyclerView.post { updateCoHostList(list) }
//        }
//    }

    private val userHandler = object : UserHandler() {
        override fun onUserUpdated(userInfo: AgoraEduContextUserInfo, operator: AgoraEduContextUserInfo?,
                                   reason: EduContextUserUpdateReason?) {
            super.onUserUpdated(userInfo, operator, reason)
            // todo 判断userInfo中的isCoHost、rewardCount和differ.currentList中的缓存是否匹配，不匹配就刷新
        }
    }

    private val mediaHandler = object : MediaHandler3() {
        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            super.onVolumeUpdated(volume, streamUuid)
            updateAudioVolumeIndication(volume, streamUuid)
        }
    }

    fun dimissDialog(recyclerView: RecyclerView) {
        var stuList = recyclerView.adapter?.itemCount!!
        var viewHolder: VideoHolderArt?
        for (i in 0..stuList) {
            viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? VideoHolderArt
            viewHolder?.videoUi?.mDialog?.dismiss()
        }
    }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemRangeInserted(position, count)
            }
            dimissDialog(recyclerView)
        }

        override fun onRemoved(position: Int, count: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemRangeRemoved(position, count)
            }
            dimissDialog(recyclerView)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            recyclerView.post {
                mVideoAdapter.notifyItemMoved(fromPosition, toPosition)
            }
            dimissDialog(recyclerView)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (recyclerView.findViewHolderForAdapterPosition(position) as? VideoHolderArt)?.let { holder ->
                payload?.let { payload ->
                    if (payload is Pair<*, *>) {
                        (payload.second as? VideoItemArt)?.let { item ->
                            holder.bind(item, eduContext)
                        }
                    }
                }
            }
        }
    }

    private val videoItemMatcher = VideoListItemMatcherArt()

    private val differ = AsyncListDiffer(
            listUpdateCallback, AsyncDifferConfig.Builder(videoItemMatcher).build())

    init {
        initView()
        eduContext?.userContext()?.addHandler(userHandler)
        eduContext?.mediaContext()?.addHandler(mediaHandler)
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

    private fun initRvAdapter(rv: RecyclerView) {//init adapter of the recyclerview
        mVideoAdapter = CoHostVideoAdapterArt(itemShadowWidth, object : IAgoraUIUserListListener {
            override fun onMuteVideo(mute: Boolean) {

            }

            override fun onMuteAudio(mute: Boolean) {

            }

            override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
                render(viewGroup, streamUuid)
            }
        })

        rv.adapter = mVideoAdapter
    }

    private fun render(viewGroup: ViewGroup?, streamUuid: String) {
        val noneView = viewGroup == null
        val isLocal = isLocalStream(streamUuid)
        if (noneView && isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (noneView && !isLocal) {
            eduContext?.mediaContext()?.stopRenderVideo(streamUuid)
        } else if (!noneView && isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid)
        } else if (!noneView && !isLocal) {
            eduContext?.mediaContext()?.startRenderVideo(EduContextRenderConfig(mirrorMode =
            EduContextMirrorMode.ENABLED), viewGroup!!, streamUuid)
        }
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        val localUserUuid = eduContext?.userContext()?.getLocalUserInfo()?.userUuid
        val info = eduContext?.streamContext()?.getAllStreamList()?.find {
            it.streamUuid == streamUuid && it.owner.userUuid == localUserUuid
        }
        return info != null
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

    private fun adjustRvItemAnimator(rv: RecyclerView) {
        rv.itemAnimator = FadeInDownAnimator()
        rv.itemAnimator?.addDuration = 600
        rv.itemAnimator?.removeDuration = 600
        rv.itemAnimator?.moveDuration = 600
        rv.itemAnimator?.changeDuration = 600
    }

    private fun bindArrowWithRv(leftArrow: ImageView, rightArrow: ImageView, rv: RecyclerView) {
        val updateRun = Runnable {
            val videoW = calculateVideoW()
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

        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View,
                                        parent: RecyclerView, state: RecyclerView.State) {
                outRect.right = itemMargin
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
        if (lastItemPosition < 0) {
            return 0
        }
        val itemCount = layoutManager.itemCount
        val recycleViewWidth: Int = rv.width
        val itemWidth = lastVisibleItem.width
        val lastItemRight = layoutManager.getDecoratedRight(lastVisibleItem)
        val distance = (itemCount - lastItemPosition - 1) * itemWidth - recycleViewWidth + lastItemRight
        return distance
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
        val distance = firstItemPosition * itemWidth - firstItemLeft
        return distance
    }

    private fun updateCoHostList(list: MutableList<EduContextUserDetailInfo>) {
        if (list.size > 0) {
            // if roomType is largeClass, localUser`s video is first.
            if (eduContext?.roomContext()?.getRoomInfo()?.roomType == EduContextRoomType.LargeClass) {
                val userInfo = eduContext.userContext()?.getLocalUserInfo()
                val item = list.find { it.user.userUuid == userInfo?.userUuid }
                item?.let {
                    list.remove(it)
                    list.add(0, it)
                }
            }
            differ.submitList(list.map { VideoItemArt(it, 0) } as MutableList<VideoItemArt>)
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
                    if (curHolder != null && curHolder is VideoHolderArt) {
                        curHolder.updateAudioVolumeIndication(value, streamUuid)
                    }
                    break
                }
                index++
            }
        }
    }

    private fun calculateVideoW(): Float {
        val videoW = if (AgoraUIConfig.isLargeScreen) {
            AgoraUIConfig.SmallClass.videoListVideoWidth * AgoraUIConfig.videoRatio1
        } else {
            AgoraUIConfig.SmallClass.videoListVideoWidth * AgoraUIConfig.videoRatio1
        }
        return videoW
    }

    private inner class CoHostVideoAdapterArt(val shadowWidth: Float,
                                              val callback: IAgoraUIUserListListener?
    ) : RecyclerView.Adapter<VideoHolderArt>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolderArt {
            val container = if (AgoraUIConfig.isLargeScreen) {
                val layout = RatioRelativeLayout(parent.context)
                layout.setRatio(1.0f / AgoraUIConfig.videoRatio1)
                layout
            } else {
//                SquareRelativeLayout(parent.context)
                val layout = RatioRelativeLayout(parent.context)
                layout.setRatio(1.0f / AgoraUIConfig.videoRatio1)
                layout
            }

            return VideoHolderArt(container, AgoraUIVideoArt(parent.context,
                    container,eduContext, 0f, 0f, shadowWidth), callback)
        }

        override fun onBindViewHolder(holder: VideoHolderArt, position: Int) {
            holder.bind(differ.currentList[holder.adapterPosition], eduContext)
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }

    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        var stuList = recyclerView.adapter?.itemCount!!
        var viewHolder: VideoHolderArt?
        for (i in 0..stuList) {

            viewHolder = recyclerView.findViewHolderForAdapterPosition(i) as? VideoHolderArt//回调onMove

            if (userDetailInfo == viewHolder?.videoUi?.userDetailInfo) {
                viewHolder?.videoUi?.setVisibility(visibility, userDetailInfo)
            }
        }
    }
}

internal data class VideoItemArt(
        var info: io.agora.agoraeducore.core.context.EduContextUserDetailInfo,
        var audioVolume: Int
)

internal class VideoListItemMatcherArt : DiffUtil.ItemCallback<VideoItemArt>() {
    override fun areItemsTheSame(oldItem: VideoItemArt, newItem: VideoItemArt): Boolean {
        return (oldItem.info.user.userUuid == newItem.info.user.userUuid)
    }

    override fun areContentsTheSame(oldItem: VideoItemArt, newItem: VideoItemArt): Boolean {
        return (oldItem.info.user.userName == newItem.info.user.userName
                && oldItem.info.onLine == newItem.info.onLine
                && oldItem.info.coHost == newItem.info.coHost
                && oldItem.info.isWaving == newItem.info.isWaving
                && oldItem.info.boardGranted == newItem.info.boardGranted
                && oldItem.info.cameraState == newItem.info.cameraState
                && oldItem.info.microState == newItem.info.microState
                && oldItem.info.enableAudio == newItem.info.enableAudio
                && oldItem.info.enableVideo == newItem.info.enableVideo
                && oldItem.info.silence == newItem.info.silence
                && oldItem.info.rewardCount == newItem.info.rewardCount
                && oldItem.audioVolume == newItem.audioVolume)
    }

    override fun getChangePayload(oldItem: VideoItemArt, newItem: VideoItemArt): Any {
        return Pair(oldItem, newItem)
    }
}

internal class VideoHolderArt(view: View,
                              val videoUi: AgoraUIVideoArt,
                              val callback: IAgoraUIUserListListener?
) : RecyclerView.ViewHolder(view), IAgoraUIVideoListener {

    fun bind(item: VideoItemArt, eduContext: EduContextPool?) {
        videoUi.videoListener = this
        videoUi.upsertUserDetailInfo(item.info, eduContext!!)// AgoraUIVideo相关UI更新
        videoUi.updateAudioVolumeIndication(item.audioVolume, item.info.streamUuid)
    }

    fun updateAudioVolumeIndication(volume: Int, streamUuid: String) {
        videoUi.updateAudioVolumeIndication(volume, streamUuid)
    }

    override fun onUpdateVideo(streamUuid: String, enable: Boolean) {
        callback?.onMuteVideo(!enable)
    }

    override fun onUpdateAudio(streamUuid: String, enable: Boolean) {
        callback?.onMuteAudio(!enable)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        callback?.onRendererContainer(viewGroup, streamUuid)
    }
}