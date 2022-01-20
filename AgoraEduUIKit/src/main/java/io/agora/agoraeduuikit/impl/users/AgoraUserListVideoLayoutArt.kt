package io.agora.agoraeduuikit.impl.users

import android.content.Context
import android.graphics.Rect
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.RatioRelativeLayout
import io.agora.agoraeduuikit.component.adapteranimator.FadeInDownAnimator
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.studentVideoHeight
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig.LargeClass.studentVideoWidth
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionPacket
import io.agora.agoraeduuikit.impl.video.AgoraLargeWindowInteractionSignal
import io.agora.agoraeduuikit.impl.video.AgoraUIVideoArt2
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIUserListListener
import io.agora.agoraeduuikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListenerImpl

class AgoraUserListVideoLayoutArt2(context: Context,
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
    private lateinit var mVideoAdapter: CoHostVideoAdapterArt2
    private val layout = LayoutInflater.from(context).inflate(R.layout.agora_userlist_video_layout, parent, false)
    private val volumeUpdateRun = VolumeUpdateRun()
    private val waveStateUpdateRun = WaveStateUpdateRun()
    private val recyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view)
    private var localUserInfo: AgoraEduContextUserInfo? = null
    private var showedLargeWindowUser: AgoraUIUserDetailInfo? = null

    private val largeWindowObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            if (id == AgoraWidgetDefaultId.LargeWindow.id) {
                val packet = Gson().fromJson(msg, AgoraLargeWindowInteractionPacket::class.java)
                if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStopRender) {
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        if (showedLargeWindowUser?.userUuid == userDetailInfo.userUuid) {
                            showedLargeWindowUser = null
                            updateUserInfo(userDetailInfo)
                        }
                    } ?: Runnable {
                        Constants.AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                } else if (packet.signal == AgoraLargeWindowInteractionSignal.LargeWindowStartRender) {
                    (Gson().fromJson(packet.body.toString(), AgoraUIUserDetailInfo::class.java))?.let { userDetailInfo ->
                        showedLargeWindowUser = userDetailInfo
                        updateUserInfo(userDetailInfo)
                    } ?: Runnable {
                        Constants.AgoraLog.e("$tag->${packet.signal}, packet.body convert failed")
                    }
                }
            }
        }
    }
    val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onCoHostListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onCoHostListChanged(userList)
            recyclerView.post { updateCoHostList(userList.toMutableList()) }
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
            (recyclerView.findViewHolderForAdapterPosition(position) as? VideoHolderArt2)?.let { holder ->
                payload?.let { payload ->
                    if (payload is Pair<*, *>) {
                        (payload.second as? VideoItemArt2)?.let { item ->
                            holder.bind(item, eduContext)
                        }
                    }
                }
            }
        }
    }

    private val videoItemMatcher = VideoListItemMatcherArt2()

    private val differ = AsyncListDiffer(
        listUpdateCallback, AsyncDifferConfig.Builder(videoItemMatcher).build())

    init {
        localUserInfo = eduContext?.userContext()?.getLocalUserInfo()
        initView()
        eduContext?.widgetContext()?.addWidgetMessageObserver(
            largeWindowObserver, AgoraWidgetDefaultId.LargeWindow.id)
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
        mVideoAdapter = CoHostVideoAdapterArt2(itemShadowWidth, object : IAgoraUIUserListListener {
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
        val localUserUuid = localUserInfo?.userUuid
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
                val lp = view.layoutParams
                val roomType = eduContext?.roomContext()?.getRoomInfo()?.roomType
                    ?: EduContextRoomType.LectureHall
                if (roomType == EduContextRoomType.LectureHall) {
                    lp.width = studentVideoWidth
                    lp.height = studentVideoHeight
                } else {
                    lp.width = parent.height
                }
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

    private fun updateCoHostList(list: MutableList<AgoraUIUserDetailInfo>) {
        if (list.size > 0) {
            differ.submitList(list.map { VideoItemArt2(it, 0) } as MutableList<VideoItemArt2>)
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

    override fun setRect(rect: Rect) {
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
                        if (curHolder != null && curHolder is VideoHolderArt2) {
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
                            curHolder.updateUserWaveState(waving)
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

    private inner class CoHostVideoAdapterArt2(val shadowWidth: Float,
                                               val callback: IAgoraUIUserListListener?
    ) : RecyclerView.Adapter<VideoHolderArt2>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolderArt2 {
            val container = if (AgoraUIConfig.keepVideoListItemRatio) {
                val layout = RatioRelativeLayout(parent.context)
                layout.setRatio(1.0f / AgoraUIConfig.videoRatio1)
                layout
            } else {
                val layout = RelativeLayout(parent.context)
                layout
            }

            return VideoHolderArt2(container, AgoraUIVideoArt2(parent.context,
                container, eduContext, 0f, 0f, shadowWidth, localUserInfo), callback)
        }

        override fun onBindViewHolder(holder: VideoHolderArt2, position: Int) {
            holder.bind(differ.currentList[holder.adapterPosition], eduContext)
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }

    fun updateUserInfo(userDetailInfo: AgoraUIUserDetailInfo?) {
        for ((index, item) in differ.currentList.withIndex()) {
            if (item.info.userUuid == userDetailInfo?.userUuid) {
                recyclerView.post { recyclerView.adapter?.notifyItemChanged(index) }
            }
        }
    }

    internal inner class VideoHolderArt2(view: View,
                                         val videoUi: AgoraUIVideoArt2,
                                         val callback: IAgoraUIUserListListener?
    ) : RecyclerView.ViewHolder(view), IAgoraUIVideoListener {
        fun bind(item: VideoItemArt2, eduContext: EduContextPool?) {
            videoUi.videoListener = this
            val renderVideo = item.info.userUuid != showedLargeWindowUser?.userUuid
            videoUi.upsertUserDetailInfo(item.info, eduContext!!, renderVideo)
            videoUi.updateAudioVolumeIndication(item.audioVolume, item.info.streamUuid)
        }

        fun updateAudioVolumeIndication(volume: Int, streamUuid: String) {
            videoUi.updateAudioVolumeIndication(volume, streamUuid)
        }

        fun updateUserWaveState(waving: Boolean) {
            videoUi.updateWaveState(waving)
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
}

internal data class VideoItemArt2(
    var info: AgoraUIUserDetailInfo,
    var audioVolume: Int
)

internal class VideoListItemMatcherArt2 : DiffUtil.ItemCallback<VideoItemArt2>() {
    override fun areItemsTheSame(oldItem: VideoItemArt2, newItem: VideoItemArt2): Boolean {
        return (oldItem.info.userUuid == newItem.info.userUuid)
    }

    override fun areContentsTheSame(oldItem: VideoItemArt2, newItem: VideoItemArt2): Boolean {
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

    override fun getChangePayload(oldItem: VideoItemArt2, newItem: VideoItemArt2): Any {
        return Pair(oldItem, newItem)
    }
}