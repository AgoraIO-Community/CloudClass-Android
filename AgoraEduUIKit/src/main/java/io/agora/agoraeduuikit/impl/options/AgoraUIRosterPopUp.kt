package io.agora.agoraeduuikit.impl.options

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
import com.google.gson.Gson
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.AgoraEduContextMediaSourceState
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Audio
import io.agora.agoraeducore.core.context.AgoraEduContextMediaStreamType.Video
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId.WhiteBoard
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.users.RosterType
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardGrantData
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.BoardGrantDataChanged
import kotlin.math.min

class AgoraUIRosterPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val rewardCount = 1
    private val tag = "AgoraUIRosterPopUp"

    private var parent: ViewGroup? = null

    private var recyclerView: RecyclerView? = null
    private var userListAdapter: UserListAdapter? = null
    private var tvTeacherName: TextView? = null

    private var eduContext: EduContextPool? = null
    private var rosterType = RosterType.SmallClass
    private var role: AgoraEduContextUserRole = AgoraEduContextUserRole.Student

    fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
    }

    fun initView(parent: ViewGroup, width: Int, height: Int, role: AgoraEduContextUserRole) {
        this.parent = parent
        this.role = role
        LayoutInflater.from(parent.context).inflate(getLayoutRes(this.rosterType), this)

        findViewById<RelativeLayout>(R.id.user_list_content_layout)?.let { layout ->
            (layout.layoutParams as? MarginLayoutParams)?.let { param ->
                param.width = width
                param.height = height
                layout.layoutParams = param
            }
        }

        recyclerView = findViewById(R.id.recycler_view)
        tvTeacherName = findViewById(R.id.tv_teacher_name)

        if (role == AgoraEduContextUserRole.Student) {
            findViewById<View>(R.id.userlist_title_kickout)?.visibility = GONE
        }

        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onUserCoHostStateChanged(item: AgoraUIUserDetailInfo, isCoHost: Boolean) {
                if (isCoHost) {
                    //eduContext?.userContext()?.addCoHost(item.userUuid)
                } else {
                    //eduContext?.userContext()?.removeCoHost(item.userUuid)
                }
            }

            override fun onAccessStateChanged(item: AgoraUIUserDetailInfo, hasAccess: Boolean) {
                val data = AgoraBoardGrantData(hasAccess, arrayOf(item.userUuid).toMutableList())
                val packet = AgoraBoardInteractionPacket(BoardGrantDataChanged, data)
                eduContext?.widgetContext()?.sendMessageToWidget(Gson().toJson(packet), WhiteBoard.id)
            }

            override fun onCameraEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean) {
                //eduContext?.streamContext()?.muteStreams(mutableListOf(), Video)
            }

            override fun onMicEnabled(item: AgoraUIUserDetailInfo, enabled: Boolean) {
                //eduContext?.streamContext()?.muteStreams(mutableListOf(), Audio)
            }

            override fun onReward(item: AgoraUIUserDetailInfo, count: Int) {
                //eduContext?.userContext()?.rewardUsers(arrayOf(item.userUuid).toMutableList(), count)
            }

            override fun onUserKickout(item: AgoraUIUserDetailInfo) {
                OptionsLayout.listener?.onKickout(item.userUuid, item.userName)
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
    }

    fun setType(type: RosterType) {
        this.rosterType = type
    }

    private fun getLayoutRes(type: RosterType) = when (type) {
        RosterType.SmallClass -> R.layout.agora_userlist_dialog_layout1
        RosterType.LargeClass -> R.layout.agora_userlist_largeclass_dialog_layout
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

        post { userListAdapter?.submitList(ArrayList(studentList)) }
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
    private fun createItemViewHolder(type: RosterType,
                                     parent: ViewGroup,
                                     listener: UserItemClickListener
    ): BaseUserHolder {
        // Roster popup has a slightly different UI design so it
        // uses another layout xml for all roster types
        return ClassUserHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.agora_userlist_dialog_list_item1, null),
                role == AgoraEduContextUserRole.Student, listener)
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
        private val type: RosterType,
        val view: View, val listener: UserItemClickListener
    ) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: AgoraUIUserDetailInfo)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener)
        : ListAdapter<AgoraUIUserDetailInfo, BaseUserHolder>(UserListDiff()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                createItemViewHolder(rosterType, parent, listener)

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

    private inner class ClassUserHolder(view: View,
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

            accessIcon.isEnabled = item.whiteBoardGranted
            if (!isStudent) {
                view.findViewById<ViewGroup>(R.id.roster_access_touch_area)?.let {
                    it.setOnClickListener {
//                        listener.onAccessStateChanged(item, !item.boardGranted)////todo set board granted state
                        listener.onAccessStateChanged(item, !true)
                    }
                }
            }

            handleCameraState(view, item, isStudent, listener)
            handleMicState(view, item, isStudent, listener)

            val tmp = min(item.reward, 99)
            startIcon.text = view.resources.getString(R.string.agora_video_reward, tmp)
            if (!isStudent) {
                view.findViewById<RelativeLayout>(R.id.agora_roster_list_item_star_layout)?.let {
                    it.setOnClickListener {
                        listener.onReward(item, rewardCount)
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

        private fun handleCameraState(layout: View, item: AgoraUIUserDetailInfo,
                                      isStudent: Boolean, listener: UserItemClickListener) {
            if (item.isCoHost) {
                if (item.videoSourceState == AgoraEduContextMediaSourceState.Open){
                    cameraIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_camera_switch_cohost))
                    cameraIcon.isActivated = (item.videoSourceState == AgoraEduContextMediaSourceState.Open) && item.hasVideo
                }else{
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
                        area.postDelayed({ area.isClickable = true }, AgoraUIConfig.clickInterval)
                        listener.onCameraEnabled(item, !cameraIcon.isActivated)
                    }
                }
            }
        }

        private fun handleMicState(layout: View, item: AgoraUIUserDetailInfo,
                                   isStudent: Boolean, listener: UserItemClickListener) {

            if (item.isCoHost) {
                if (item.audioSourceState == AgoraEduContextMediaSourceState.Open){
                    micIcon.setImageDrawable(context.getDrawable(R.drawable.agora_userlist_mic_switch_cohost))
                    micIcon.isActivated = (item.audioSourceState == AgoraEduContextMediaSourceState.Open) && item.hasAudio
                }else{
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
                        area.postDelayed({ area.isClickable = true }, AgoraUIConfig.clickInterval)
                        listener.onMicEnabled(item, !micIcon.isActivated)
                    }
                }
            }
        }
    }
}