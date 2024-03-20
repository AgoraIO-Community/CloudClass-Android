package io.agora.online.options

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.*
import io.agora.online.component.AgoraEduCarouselControlComponent
import io.agora.online.component.common.AbsAgoraEduComponent
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.helper.sort
import io.agora.online.options.bean.Data
import com.google.gson.Gson
import io.agora.agoraeducore.core.AgoraEduCoreConfig
import io.agora.agoraeducore.core.context.*
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId.WhiteBoard
import io.agora.online.R
import io.agora.online.component.dialog.AgoraUICustomDialogBuilder
import io.agora.online.helper.clearPinyinCacheMap
import io.agora.online.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal
import io.agora.online.provider.AgoraUIUserDetailInfo
import io.agora.online.provider.UIDataProviderListenerImpl
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min

/**
 * author : wufang
 * date :
 * description :花名册组件
 */
class AgoraEduRosterComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    var isOk = true //是否加载完成
    private var curPageNo = 1//当前加载第一页数据
    private val rewardCount = 1
    private val tag = "AgoraUIRosterPopUp"
    private var curUserList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()

    private var parent: ViewGroup? = null

    private var recyclerView: RecyclerView? = null
    private var userListAdapter: UserListAdapter? = null
    private var tvTeacherName: TextView? = null
    private var mConfig: AgoraEduCoreConfig? = null //获取launchConfig参数
    private var rosterType = RoomType.SMALL_CLASS
    private var role: AgoraEduContextUserRole? = AgoraEduContextUserRole.Student
    private val userService = AppRetrofitManager.getService(RosterService::class.java)
    private var pool: ExecutorService = Executors.newSingleThreadExecutor()
    var isShow = false // 是否显示了花名册
    var isPreload = true // 是否预加载了

    private val uiDataProviderListener = object : UIDataProviderListenerImpl() {
        override fun onUserListChanged(userList: List<AgoraUIUserDetailInfo>) {
            super.onUserListChanged(userList)
            curUserList = userList as MutableList<AgoraUIUserDetailInfo>
            if (isShow || isPreload) {
                if (curUserList.isNotEmpty()) {
                    isPreload = false
                }
                showUserList()
            }
        }
    }

    fun getStuListInRoster(pageNo: Int) {
        val call = userService.getUsersInRoster(mConfig?.appId!!, mConfig?.roomUuid!!, pageNo)

        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Data>>() {
            override fun onSuccess(res: HttpBaseRes<Data>?) {
                var stuList: MutableList<AgoraUIUserDetailInfo> = mutableListOf()
                stuList.clear()
                res?.data?.agoraEduRosterUserList?.forEach {
                    var role2 =
                        if (it.role == "host") AgoraEduContextUserRole.Teacher else AgoraEduContextUserRole.Student
                    var hasAudio = false
                    var hasVideo = false
                    var streamUuid = ""
                    var audioSourceState = AgoraEduContextMediaSourceState.Close
                    var videoSourceState = AgoraEduContextMediaSourceState.Close
                    if (it.streams?.size != 0) {
                        hasAudio = it.streams?.get(0)?.audioState == 1
                        hasVideo = it.streams?.get(0)?.videoState == 1
                        streamUuid = it.streams?.get(0)?.streamUuid!!
                        audioSourceState =
                            if (it.streams?.get(0)?.audioSourceState!! == 1) AgoraEduContextMediaSourceState.Open else AgoraEduContextMediaSourceState.Close
                        videoSourceState =
                            if (it.streams?.get(0)?.videoSourceState!! == 1) AgoraEduContextMediaSourceState.Open else AgoraEduContextMediaSourceState.Close
                    }
                    var user = AgoraUIUserDetailInfo(
                        it.userUuid!!,
                        it.userName!!,
                        role2,
                        false//isCoHost
                        , 0//reward
                        , false//whiteBoardGranted
                        , false,//isLocal
                        hasAudio,//hasAudio
                        hasVideo,//hasVideo
                        streamUuid,//streamUuid
                        "",//streamName
                        AgoraEduContextMediaStreamType.None,
                        AgoraEduContextAudioSourceType.None,
                        AgoraEduContextVideoSourceType.None,
                        audioSourceState,
                        videoSourceState
                    )
                    stuList.add(user)
                }
                if (stuList.size != 0) {
                    onUserListUpdated(stuList)
                    isOk = true
                }
                LogX.e(tag, "${res?.let { GsonUtil.toJson(it) }}")
            }
        })
    }

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        mConfig = agoraUIProvider.getAgoraEduCore()?.config
        val mRoomType = eduContext?.roomContext()?.getRoomInfo()?.roomType
        mRoomType?.let { setType(it) }
        role = eduContext?.userContext()?.getLocalUserInfo()?.role
        LayoutInflater.from(context).inflate(getLayoutRes(this.rosterType), this)
        if (mRoomType == RoomType.LARGE_CLASS) {
            findViewById<AgoraEduCarouselControlComponent>(R.id.student_carousel_component).visibility = GONE
        } else {
            findViewById<AgoraEduCarouselControlComponent>(R.id.student_carousel_component).initView(agoraUIProvider)
        }
//        findViewById<RelativeLayout>(R.id.user_list_content_layout)?.let { layout ->
//            (layout.layoutParams as? MarginLayoutParams)?.let { param ->
//                param.width = MarginLayoutParams.MATCH_PARENT
//                param.height = MarginLayoutParams.MATCH_PARENT
//                layout.layoutParams = param
//            }
//        }

        recyclerView = findViewById(R.id.roster_recycler_view)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView?.layoutManager = linearLayoutManager
        tvTeacherName = findViewById(R.id.tv_teacher_name)

        if (role == AgoraEduContextUserRole.Student) {
            findViewById<View>(R.id.userlist_title_kickout)?.visibility = GONE
        }
        if (mRoomType == RoomType.LARGE_CLASS) {
            findViewById<View>(R.id.userlist_title_cohost)?.visibility = GONE
            findViewById<View>(R.id.userlist_title_whiteboard)?.visibility = GONE
            findViewById<View>(R.id.userlist_title_reward)?.visibility = GONE
        }

        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onUserCoHostStateChanged(item: AgoraUIUserDetailInfo, isCoHost: Boolean) {
                if (isCoHost) {
                    eduContext?.userContext()?.addCoHost(item.userUuid)
                } else {
                    eduContext?.userContext()?.removeCoHost(item.userUuid)
                }
            }

            override fun onAccessStateChanged(item: AgoraUIUserDetailInfo, hasAccess: Boolean) {
                val data = AgoraBoardGrantData(hasAccess, arrayOf(item.userUuid).toMutableList())
                val packet = AgoraBoardInteractionPacket(AgoraBoardInteractionSignal.BoardGrantDataChanged, data)
                eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), WhiteBoard.id)
            }

            override fun onCameraEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean) {
                if (enabled) {
                    eduContext?.streamContext()?.publishStreams(arrayOf(item.streamUuid).toMutableList(), AgoraEduContextMediaStreamType.Video)
                } else {
                    eduContext?.streamContext()?.muteStreams(arrayOf(item.streamUuid).toMutableList(), AgoraEduContextMediaStreamType.Video)
                }
            }

            override fun onMicEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean) {
                if (enabled) {
                    eduContext?.streamContext()?.publishStreams(arrayOf(item.streamUuid).toMutableList(), AgoraEduContextMediaStreamType.Audio)
                } else {
                    eduContext?.streamContext()?.muteStreams(arrayOf(item.streamUuid).toMutableList(), AgoraEduContextMediaStreamType.Audio)
                }
            }

            override fun onReward(item: AgoraUIUserDetailInfo, count: Int) {
                eduContext?.userContext()?.rewardUsers(arrayOf(item.userUuid).toMutableList(), count)
            }

            override fun onUserKickout(item: AgoraUIUserDetailInfo) {
                showKickDialog(item.userUuid)
            }
        })

        recyclerView?.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(context, R.drawable.agora_userlist_divider)!!)
            })

        recyclerView?.addItemDecoration(object : RecyclerView.ItemDecoration() {
            val itemHeight = context.resources.getDimensionPixelSize(R.dimen.agora_userlist_row_height)
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val layoutParams = view.layoutParams
                layoutParams.width = parent.measuredWidth
                layoutParams.height = itemHeight
                view.layoutParams = layoutParams
                super.getItemOffsets(outRect, view, parent, state)
            }
        })

        // remove the animator when refresh item
        recyclerView?.itemAnimator?.addDuration = 0
        recyclerView?.itemAnimator?.changeDuration = 0
        recyclerView?.itemAnimator?.moveDuration = 0
        recyclerView?.itemAnimator?.removeDuration = 0
        (recyclerView?.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        recyclerView?.adapter = userListAdapter
        recyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                var lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition()
                var sum = userListAdapter!!.itemCount
                if (newState == RecyclerView.SCROLL_STATE_IDLE
                    && lastVisibleItemPosition + 1 == sum && sum >= 100
                ) {
                    if (isOk) {
                        isOk = false
                        LogX.i(tag,"try to load more data")
                        getStuListInRoster(++curPageNo)
                    }
                }

            }
        })

        agoraUIProvider.getUIDataProvider()?.addListener(uiDataProviderListener)
        agoraUIProvider.getUIDataProvider()?.notifyUserListChanged()
        showUserList()
    }

    fun showUserList() {
        pool.execute {
            curUserList = sort(curUserList)
            onUserListUpdated(curUserList)
        }
    }

    fun updateStuListData() {
        curPageNo = 1
        getStuListInRoster(curPageNo)
    }

    private fun showKickDialog(userId: String) {
        this.let {
            it.post {
                val customView = LayoutInflater.from(it.context).inflate(
                    R.layout.fcr_online_kick_dialog_radio_layout, it, false
                )
                val optionOnce = customView.findViewById<RelativeLayout>(R.id.agora_kick_dialog_once_layout)
                val optionForever = customView.findViewById<RelativeLayout>(R.id.agora_kick_dialog_forever_layout)
                optionOnce.isActivated = true
                optionForever.isActivated = false
                optionOnce.setOnClickListener {
                    optionOnce.isActivated = true
                    optionForever.isActivated = false
                }
                optionForever.setOnClickListener {
                    optionOnce.isActivated = false
                    optionForever.isActivated = true
                }
                AgoraUICustomDialogBuilder(it.context)
                    .title(it.context.resources.getString(R.string.fcr_user_kick_out))
                    .negativeText(it.context.resources.getString(R.string.fcr_user_kick_out_cancel))
                    .positiveText(it.context.resources.getString(R.string.fcr_user_kick_out_submit))
                    .positiveClick {
                        val forever = !optionOnce.isActivated && optionForever.isActivated
                        eduContext?.userContext()?.kickOutUser(userId, forever)
                    }
                    .setCustomView(customView)
                    .build()
                    .show()
            }
        }
    }

    private fun setType(type: RoomType) {
        this.rosterType = type
    }

    private fun getLayoutRes(type: RoomType) = when (type) {
        RoomType.SMALL_CLASS -> R.layout.fcr_online_userlist_dialog_layout
        RoomType.LARGE_CLASS -> R.layout.fcr_online_userlist_dialog_layout
        else -> R.layout.fcr_online_userlist_dialog_layout
    }

    fun dismiss() {
        parent?.let { parent ->
            var contains = false
            parent.forEach {
                if (it == this) contains = true
            }
            if (contains) parent.removeView(this)
            this.removeAllViews()
        }
    }

    override fun release() {
        super.release()
        clearPinyinCacheMap()
        agoraUIProvider.getUIDataProvider()?.removeListener(uiDataProviderListener)
    }

    fun onUserListUpdated(list: MutableList<AgoraUIUserDetailInfo>) {
        updateUserListAdapter(list)
    }

    private fun updateUserListAdapter(list: MutableList<AgoraUIUserDetailInfo>) {
        val studentList = mutableListOf<AgoraUIUserDetailInfo>()
        list.forEach { item ->
            if (item.role == AgoraEduContextUserRole.Student) {
                studentList.add(item)
            } else if (item.role == AgoraEduContextUserRole.Teacher) {
                updateTeacher(item)
            }
        }
        post { userListAdapter?.submitList(ArrayList(studentList)) }//更新学生列表
    }

    private fun updateTeacher(info: AgoraUIUserDetailInfo) {
        tvTeacherName?.post { tvTeacherName?.text = info.userName }
    }

    private fun updateStudent(info: AgoraUIUserDetailInfo) {
        val index = findIndex(info)
        if (index >= 0) {
            userListAdapter?.currentList?.set(index, info)
            userListAdapter?.notifyItemChanged(index)
        }
    }

    private fun findIndex(info: AgoraUIUserDetailInfo): Int {
        var index = 0
        var foundIndex = -1;
        for (item in userListAdapter?.currentList!!) {
            if (item.userUuid == info.userUuid) {
                foundIndex = index
                break
            }
            index++
        }
        return foundIndex
    }

    @SuppressLint("InflateParams")
    private fun createItemViewHolder(
        parent: ViewGroup,
        listener: UserItemClickListener
    ): BaseUserHolder {
        // Roster popup has a slightly different UI design so it
        // uses another layout xml for all roster types
        return ClassUserHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.fcr_online_userlist_dialog_list_item, null),
            role == AgoraEduContextUserRole.Student, listener
        )
    }

    private class UserListDiff : DiffUtil.ItemCallback<AgoraUIUserDetailInfo>() {
        override fun areItemsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
            return oldItem == newItem && oldItem.userUuid == newItem.userUuid
        }

        override fun areContentsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
            return oldItem.userName == newItem.userName
                && oldItem.role == newItem.role
                && oldItem.isCoHost == newItem.isCoHost
                && oldItem.reward == newItem.reward
                && oldItem.hasAudio == newItem.hasAudio
                && oldItem.hasVideo == newItem.hasVideo
                && oldItem.streamUuid == newItem.streamUuid
                && oldItem.streamName == newItem.streamName
        }
    }

    private abstract inner class BaseUserHolder(
        private val type: RoomType,
        val view: View, val listener: UserItemClickListener
    ) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: AgoraUIUserDetailInfo)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener) : ListAdapter<AgoraUIUserDetailInfo, BaseUserHolder>(
        UserListDiff()
    ) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            createItemViewHolder(parent, listener)

        override fun onBindViewHolder(holder: BaseUserHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private interface UserItemClickListener {
        fun onUserCoHostStateChanged(item: AgoraUIUserDetailInfo, isCoHost: Boolean)
        fun onAccessStateChanged(item: AgoraUIUserDetailInfo, hasAccess: Boolean)
        fun onCameraEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean)
        fun onMicEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean)
        fun onReward(item: AgoraUIUserDetailInfo, count: Int)
        fun onUserKickout(item: AgoraUIUserDetailInfo)
    }

    private inner class ClassUserHolder(
        view: View,
        // Local user role, not the list item's role
        private val isStudent: Boolean,
        listener: UserItemClickListener
    ) : BaseUserHolder(rosterType, view, listener) {

        private val name: TextView? = view.findViewById(R.id.roster_item_user_name)
        private val desktopIcon: AppCompatImageView = view.findViewById(R.id.roster_item_desktop_icon)
        private val accessIcon: AppCompatImageView = view.findViewById(R.id.roster_item_access_icon)
        private val cameraIcon: AppCompatImageView = view.findViewById(R.id.roster_item_camera_icon)
        private val micIcon: AppCompatImageView = view.findViewById(R.id.roster_item_mic_icon)
        private val startIcon: CheckedTextView = view.findViewById(R.id.roster_item_star_icon_text)

        override fun bind(item: AgoraUIUserDetailInfo) {
            name?.text = item.userName

            desktopIcon.isEnabled = item.isCoHost
            if (!isStudent) {
                view.findViewById<ViewGroup>(R.id.roster_item_desktop_touch_area)?.let {
                    it.setOnClickListener {
                        listener.onUserCoHostStateChanged(item, !item.isCoHost)
                    }
                }
            }

            if (rosterType == RoomType.LARGE_CLASS) {
                view.findViewById<View>(R.id.roster_item_desktop_touch_area)?.visibility = GONE
                view.findViewById<View>(R.id.agora_roster_list_item_desktop_layout)?.visibility = GONE
                view.findViewById<View>(R.id.agora_roster_list_item_access_layout)?.visibility = GONE
                view.findViewById<View>(R.id.roster_access_touch_area)?.visibility = GONE
                view.findViewById<View>(R.id.agora_roster_list_item_star_layout)?.visibility = GONE

            }

            accessIcon.isEnabled = item.whiteBoardGranted
            if (!isStudent) {
                view.findViewById<ViewGroup>(R.id.roster_access_touch_area)?.let {
                    it.setOnClickListener {
                        listener.onAccessStateChanged(item, !item.whiteBoardGranted)
                    }
                }
            }

            handleCameraState(view, item, isStudent, listener)
            handleMicState(view, item, isStudent, listener)

            val tmp = min(item.reward, 99)
            startIcon.text = view.resources.getString(R.string.fcr_agora_video_reward, tmp)
            if (!isStudent) {
                view.findViewById<RelativeLayout>(R.id.agora_roster_list_item_star_layout)?.let {
                    it.setOnClickListener {
                        listener.onReward(item, rewardCount)
                        startIcon.text = view.resources.getString(R.string.fcr_agora_video_reward, min(item.reward, 99))
                    }
                }
            }

            if (!isStudent) {
                view.findViewById<RelativeLayout>(R.id.roster_item_kickout_touch_area)?.let {
                    it.visibility = VISIBLE
                    it.setOnClickListener {
                        listener.onUserKickout(item)
                    }
                }
            } else {
                view.findViewById<RelativeLayout>(R.id.agora_roster_list_item_kickout_layout)?.let {
                    it.visibility = GONE
                }
            }
        }

        private fun handleCameraState(
            layout: View, item: AgoraUIUserDetailInfo,
            isStudent: Boolean, listener: UserItemClickListener
        ) {
            if (item.isCoHost || rosterType == RoomType.LARGE_CLASS) {
                if (item.videoSourceState == AgoraEduContextMediaSourceState.Open) {
                    cameraIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_camera_switch_cohost))
                    cameraIcon.isActivated = (item.videoSourceState == AgoraEduContextMediaSourceState.Open) && item.hasVideo
                } else {
                    cameraIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_camera_switch_uncohost))
                    cameraIcon.isActivated = false
                }
            } else {
                cameraIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_camera_switch_uncohost))
                cameraIcon.isActivated = true
            }
            if (!isStudent) {
                layout.findViewById<RelativeLayout>(R.id.roster_item_camera_touch_area)?.let { area ->
                    area.setOnClickListener {
                        area.isClickable = false
                        area.postDelayed({ area.isClickable = true }, 500L)
                        listener.onCameraEnabled(item, !cameraIcon.isActivated)
                    }
                }
            }
        }

        private fun handleMicState(
            layout: View, item: AgoraUIUserDetailInfo,
            isStudent: Boolean, listener: UserItemClickListener
        ) {
            if (item.isCoHost || rosterType == RoomType.LARGE_CLASS) {
                if (item.audioSourceState == AgoraEduContextMediaSourceState.Open) {
                    micIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_mic_switch_cohost))
                    micIcon.isActivated = (item.audioSourceState == AgoraEduContextMediaSourceState.Open) && item.hasAudio
                } else {
                    micIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_mic_switch_uncohost))
                    micIcon.isActivated = false
                }

            } else {
                micIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_mic_switch_uncohost))
                micIcon.isActivated = true
            }

            if (!isStudent) {
                layout.findViewById<RelativeLayout>(R.id.roster_item_mic_touch_area)?.let { area ->
                    area.setOnClickListener {
                        area.isClickable = false
                        area.postDelayed({ area.isClickable = true }, 500L)
                        listener.onMicEnabled(item, !micIcon.isActivated)
                    }
                }
            }
        }
    }
}

/*花名册请求学生数据*/
internal interface RosterService {
    @GET("edu/apps/{appId}/v2/rooms/{roomUuid}/users/page")
    fun getUsersInRoster(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Query("pageNo") pageNo: Int
    ): Call<HttpBaseRes<Data>>
}
