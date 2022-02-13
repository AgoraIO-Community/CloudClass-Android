package io.agora.agoraeduuikit.impl.options

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.*
import io.agora.agoraeduuikit.R
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

//the popup window shows the student list who is waving hands
class AgoraUIHandsUpListPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)
    constructor(context: Context, userList: MutableList<AgoraUIUserDetailInfo>) : super(context) {
        this.userList = userList
    }


    private val tag = "AgoraUIHandsUpListPopUp"

    private var parent: ViewGroup? = null

    private var recyclerView: RecyclerView? = null
    private var userListAdapter: UserListAdapter? = null
    private var userList: MutableList<AgoraUIUserDetailInfo> = ArrayList()
    private var eduContext: EduContextPool? = null
    private var role: AgoraEduContextUserRole = AgoraEduContextUserRole.Student

    fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
    }

    fun initView(parent: ViewGroup, role: AgoraEduContextUserRole) {
        this.parent = parent
        this.role = role
        LayoutInflater.from(parent.context).inflate(R.layout.agora_option_handsup_list_popup_layout, this)

        recyclerView = findViewById(R.id.rv_handsup_list)

        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onUserCoHostStateChanged(item: AgoraUIUserDetailInfo, isCoHost: Boolean) {
                if (isCoHost) {
                    //eduContext?.userContext()?.addCoHost(item.userUuid)
                }
            }
        })

        onUserListUpdated(this.userList)//更新adapter中的userList
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
        val studentWavingList = mutableListOf<AgoraUIUserDetailInfo>()

        list.forEach { item ->
            if (false) { //筛选出正在举手的用户
//            if (item.isWaving) { //筛选出正在举手的用户 //todo
                studentWavingList.add(item)
            }
        }

        post { userListAdapter?.submitList(ArrayList(studentWavingList)) }
        if (studentWavingList.size == 0) { //popup消失条件
            this.post {
                this?.let { pop ->
                    pop.parent?.let { parent ->
                        (parent as? ViewGroup)?.removeView(pop)
                    }
                }

            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createItemViewHolder(
            parent: ViewGroup,
            listener: UserItemClickListener
    ): BaseUserHolder {
        // Roster popup has a slightly different UI design so it
        // uses another layout xml for all roster types
        return ClassUserHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.agora_handsup_list_dialog_listitem, null), listener)
    }

    private class UserListDiff : DiffUtil.ItemCallback<AgoraUIUserDetailInfo>() {
        override fun areItemsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
            return oldItem == newItem && oldItem.userUuid == newItem.userUuid
        }

        override fun areContentsTheSame(oldItem: AgoraUIUserDetailInfo, newItem: AgoraUIUserDetailInfo): Boolean {
//            return oldItem.userName == newItem.userName
//                    && oldItem.onLine == newItem.onLine
//                    && oldItem.coHost == newItem.coHost
//                    && oldItem.boardGranted == newItem.boardGranted
//                    && oldItem.cameraState == newItem.cameraState
//                    && oldItem.microState == newItem.microState
//                    && oldItem.enableAudio == newItem.enableAudio
//                    && oldItem.enableVideo == newItem.enableVideo
//                    && oldItem.rewardCount == newItem.rewardCount
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
            val view: View, val listener: UserItemClickListener
    ) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: AgoraUIUserDetailInfo)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener)
        : ListAdapter<AgoraUIUserDetailInfo, BaseUserHolder>(UserListDiff()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                createItemViewHolder(parent, listener)

        override fun onBindViewHolder(holder: BaseUserHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private interface UserItemClickListener {
        fun onUserCoHostStateChanged(item: AgoraUIUserDetailInfo, isCoHost: Boolean)

    }


    private inner class ClassUserHolder(view: View,

                                        listener: UserItemClickListener
    ) : BaseUserHolder(view, listener) {

        private val name: TextView? = view.findViewById(R.id.tv_waving_name)
        private val desktopIcon: AppCompatImageView = view.findViewById(R.id.roster_item_desktop_icon)


        override fun bind(item: AgoraUIUserDetailInfo) {
            name?.text = item.userName

            desktopIcon.isEnabled = item.isCoHost

            view.findViewById<ViewGroup>(R.id.roster_item_desktop_touch_area)?.let {
                it.setOnClickListener {
                    if (!item.isCoHost) {//还没上台前，点击后可上台
                        listener.onUserCoHostStateChanged(item, !item.isCoHost)
                    }
                }
            }
        }

    }
}