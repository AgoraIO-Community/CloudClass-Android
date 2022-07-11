package com.agora.edu.component

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import com.agora.edu.component.helper.AgoraRendererUtils
import com.agora.edu.component.helper.AgoraUIConfig
import com.agora.edu.component.teachaids.presenter.FCRLargeWindowManager
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.adapteranimator.FadeInDownAnimator
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

/**
 * author : hefeng
 * date : 2022/1/26
 * description : 小班课学生列表
 */
class AgoraEduListVideoComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val TAG = "AgoraEduUserListVideoComponent"

    private lateinit var mVideoAdapter: CoHostVideoAdapter
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_edu_userlist_video_component, this, true)
    private val volumeUpdateRun = VolumeUpdateRun()
    private val waveStateUpdateRun = WaveStateUpdateRun()
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
    private var localUserInfo: AgoraEduContextUserInfo? = null
    var classRoomType: RoomType = RoomType.SMALL_CLASS // 教室类型

    private val itemMargin = context.resources.getDimensionPixelOffset(R.dimen.agora_video_distance)

    var roomUuid: String? = null

    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            uiHandler.post { updateCoHostList(userList.toMutableList()) }
        }

        override fun onVolumeChanged(volume: Int, streamUuid: String) {
            super.onVolumeChanged(volume, streamUuid)
            updateAudioVolumeIndication(volume, streamUuid)
        }

        override fun onUserHandsWave(userUuid: String, duration: Int, payload: Map<String, Any>?) {
            updateUserWaveState(userUuid, true)
        }

        override fun onUserHandsDown(userUuid: String, payload: Map<String, Any>?) {
            updateUserWaveState(userUuid, false)
        }
    }

    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            mVideoAdapter.notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            mVideoAdapter.notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            mVideoAdapter.notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (recyclerView.findViewHolderForAdapterPosition(position) as? VideoHolder)?.let { holder ->
                payload?.let { payload ->
                    if (payload is Pair<*, *>) {
                        (payload.second as? VideoItem)?.let { item ->
                            holder.bind(roomUuid, item, agoraUIProvider)
                        }
                    }
                }
            }
        }
    }

    private val videoItemMatcher = VideoListItemMatcher()

    private val differ = AsyncListDiffer(listUpdateCallback, AsyncDifferConfig.Builder(videoItemMatcher).build())

    init {
        val leftArrow = layout.findViewById<ImageView>(R.id.iv_arrow_left)
        val rightArrow = layout.findViewById<ImageView>(R.id.iv_arrow_right)
        bindArrowWithRv(leftArrow, rightArrow, recyclerView)
        adjustRvItemSize(recyclerView)
        adjustRvItemAnimator(recyclerView)
        initRvAdapter(recyclerView)
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        uiDataProvider?.addListener(uiDataProviderListener)
        roomUuid = eduContext?.roomContext()?.getRoomInfo()?.roomUuid
        localUserInfo = eduContext?.userContext()?.getLocalUserInfo()
    }

    private fun initRvAdapter(rv: RecyclerView) {//init adapter of the recyclerview
        mVideoAdapter = CoHostVideoAdapter(object : IAgoraUIVideoListener {
            override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
                render(viewGroup, info)
            }
        })

        rv.adapter = mVideoAdapter
    }

    private fun render(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        AgoraRendererUtils.onRendererContainer(eduCore, viewGroup, info, isLocalStream(info.streamUuid))
    }

    private fun isLocalStream(streamUuid: String): Boolean {
        val localUserUuid = localUserInfo?.userUuid
        val info = eduContext?.streamContext()?.getAllStreamList()?.find {
            it.streamUuid == streamUuid && it.owner.userUuid == localUserUuid
        }
        return info != null
    }

    fun getItemViewPosition(streamUuid: String): Rect? {
        differ.currentList.forEachIndexed { index, item ->
            if (item.info.streamUuid == streamUuid) {
                recyclerView.layoutManager?.findViewByPosition(index)?.let {
                    val rvLeft = this.left
                    return Rect(it.left + rvLeft, it.top, it.right + rvLeft, it.bottom)
                }
            }
        }
        return null
    }

    fun show(show: Boolean) {
        layout.post {
            layout.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

//    fun isShown(): Boolean {
//        return layout.visibility == View.VISIBLE
//    }

    private fun adjustRvItemSize(rv: RecyclerView) {
//        rv.addItemDecoration(object : RecyclerView.ItemDecoration() {
//            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
//                val lp = view.layoutParams
//                val roomType = eduContext?.roomContext()?.getRoomInfo()?.roomType
//                    ?: EduContextRoomType.LectureHall
//                if (roomType == EduContextRoomType.LectureHall) {
//                    lp.width = AgoraUIConfig.LargeClass.studentVideoWidth
//                    lp.height = AgoraUIConfig.LargeClass.studentVideoHeight
//                } else {
//                    lp.width = parent.height
//                }
//                view.layoutParams = lp
//                super.getItemOffsets(outRect, view, parent, state)
//            }
//        })
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
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
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
            override fun getItemOffsets(
                outRect: Rect, view: View,
                parent: RecyclerView, state: RecyclerView.State
            ) {
                outRect.right = itemMargin
            }
        })

//        leftArrow.layoutParams = leftArrow.layoutParams.apply {
//            val param = this as ViewGroup.MarginLayoutParams
//            param.topMargin = itemShadowWidth.toInt()
//            param.bottomMargin = itemShadowWidth.toInt()
//        }
//
//        rightArrow.layoutParams = rightArrow.layoutParams.apply {
//            val param = this as ViewGroup.MarginLayoutParams
//            param.topMargin = itemShadowWidth.toInt()
//            param.bottomMargin = itemShadowWidth.toInt()
//        }
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

    private fun updateCoHostList(list: MutableList<AgoraUIUserDetailInfo>) {
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

    private fun updateUserWaveState(userUuid: String, waving: Boolean) {
        layout.removeCallbacks(waveStateUpdateRun)
        waveStateUpdateRun.waving = waving
        waveStateUpdateRun.userUuid = userUuid
        recyclerView.post(waveStateUpdateRun)
    }

    inner class VolumeUpdateRun : Runnable {
        var value: Int = 0
        var streamUuid: String = ""

        override fun run() {
            if (TextUtils.isEmpty(streamUuid)) {
                return
            }
            run outside@{
                differ.currentList.forEachIndexed { index, item ->
                    if (item.info.streamUuid == streamUuid) {
                        item.audioVolume = value
                        val curHolder = recyclerView.findViewHolderForAdapterPosition(index)
                        if (curHolder != null && curHolder is VideoHolder) {
                            curHolder.updateAudioVolumeIndication(value, streamUuid)
                        }
                        return@outside
                    }
                }
            }
        }
    }

    inner class WaveStateUpdateRun : Runnable {
        var waving: Boolean = false
        var userUuid: String = ""

        override fun run() {
            if (TextUtils.isEmpty(userUuid)) {
                return
            }
            run outside@{
                differ.currentList.forEachIndexed { index, item ->
                    if (item.info.userUuid == userUuid) {
                        val curHolder = recyclerView.findViewHolderForAdapterPosition(index)
                        if (curHolder != null && curHolder is VideoHolder) {
                            curHolder.updateUserWaveState(userUuid, waving)
                        }
                        return@outside
                    }
                }
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

    private inner class CoHostVideoAdapter(val callback: IAgoraUIVideoListener?) : RecyclerView.Adapter<VideoHolder>() {

        var layoutInflater = LayoutInflater.from(context)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
            if (classRoomType == RoomType.LARGE_CLASS) {
                return VideoHolder(
                    layoutInflater.inflate(R.layout.agora_edu_userlist_video_item_v2, parent, false),
                    callback
                )
            }

            return VideoHolder(
                layoutInflater.inflate(R.layout.agora_edu_userlist_video_item, parent, false),
                callback
            )
        }

        override fun onBindViewHolder(holder: VideoHolder, position: Int) {
            val index = holder.bindingAdapterPosition
            holder.bind(roomUuid, differ.currentList[index], agoraUIProvider)
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }
}

internal data class VideoItem(
    var info: AgoraUIUserDetailInfo,
    var audioVolume: Int
)

internal class VideoListItemMatcher : DiffUtil.ItemCallback<VideoItem>() {
    override fun areItemsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return (oldItem.info.userUuid == newItem.info.userUuid)
    }

    override fun areContentsTheSame(oldItem: VideoItem, newItem: VideoItem): Boolean {
        return (oldItem.info.userName == newItem.info.userName
            && oldItem.info.role == newItem.info.role
            && oldItem.info.isCoHost == newItem.info.isCoHost
            && oldItem.info.reward == newItem.info.reward
            && oldItem.info.whiteBoardGranted == newItem.info.whiteBoardGranted
            && oldItem.info.isLocal == newItem.info.isLocal
            && oldItem.info.hasAudio == newItem.info.hasAudio
            && oldItem.info.hasVideo == newItem.info.hasVideo
            && oldItem.info.streamUuid == newItem.info.streamUuid
            && oldItem.info.streamName == newItem.info.streamName
            && oldItem.info.streamType == newItem.info.streamType
            && oldItem.info.audioSourceType == newItem.info.audioSourceType
            && oldItem.info.videoSourceType == newItem.info.videoSourceType
            && oldItem.info.audioSourceState == newItem.info.audioSourceState
            && oldItem.info.videoSourceState == newItem.info.videoSourceState
            && oldItem.audioVolume == newItem.audioVolume)
    }

    override fun getChangePayload(oldItem: VideoItem, newItem: VideoItem): Any {
        return Pair(oldItem, newItem)
    }
}


internal class VideoHolder(var view: View, var callback: IAgoraUIVideoListener?) : RecyclerView.ViewHolder(view),
    IAgoraUIVideoListener {

    var uiVideo = view.findViewById<AgoraEduVideoComponent>(R.id.agora_edu_video)

    fun bind(roomUuid: String?, item: VideoItem, agoraUIProvider: IAgoraUIProvider) {
        roomUuid?.let {
            uiVideo.largeWindowOpened = FCRLargeWindowManager.isLargeWindow(it, item.info.streamUuid)
        }
        uiVideo.videoListener = this
        uiVideo.initView(agoraUIProvider)
        uiVideo.upsertUserDetailInfo(item.info)
        uiVideo.updateAudioVolumeIndication(item.audioVolume, item.info.streamUuid)
    }

    fun updateAudioVolumeIndication(volume: Int, streamUuid: String) {
        uiVideo.updateAudioVolumeIndication(volume, streamUuid)
    }

    fun updateUserWaveState(userUuid: String, waving: Boolean) {
        uiVideo.updateWaveState(userUuid, waving)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, info: AgoraUIUserDetailInfo) {
        callback?.onRendererContainer(viewGroup, info)
    }
}




