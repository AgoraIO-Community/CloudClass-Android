package com.agora.edu.component.options

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import com.agora.edu.component.common.AbsAgoraEduComponent
import com.agora.edu.component.common.IAgoraUIProvider
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo

/**
 * author : wufang
 * date : 2022/1/19
 * description : 正在举手的学生列表
 */
//the popup window shows the student list who is waving hands
class AgoraEduHandsUpListComponent : AbsAgoraEduComponent {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraUIHandsUpListPopUp"
    private var recyclerView: RecyclerView? = null
    private var userListAdapter: UserListAdapter? = null
    private var userList: MutableList<AgoraUIUserDetailInfo> = ArrayList()

    override fun initView(agoraUIProvider: IAgoraUIProvider) {
        super.initView(agoraUIProvider)
        LayoutInflater.from(context).inflate(R.layout.agora_option_handsup_list_popup_layout, this)
        recyclerView = findViewById(R.id.rv_handsup_list)
        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onUserCoHostStateChanged(item: HandsUpUser, isCoHost: Boolean) {
                if (isCoHost) {
                    eduContext?.userContext()?.addCoHost(item.userUuid)
                }
            }
        })
//        onUserListUpdated(this.userList)//更新adapter中的userList
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


    fun onUserListUpdated(list: MutableList<HandsUpUser>) {
        updateUserListAdapter(list)
    }

    private fun updateUserListAdapter(list: MutableList<HandsUpUser>) {
        post { userListAdapter?.submitList(ArrayList(list)) }
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
                .inflate(R.layout.agora_handsup_list_dialog_listitem, null), listener
        )
    }

    private class UserListDiff : DiffUtil.ItemCallback<HandsUpUser>() {
        override fun areItemsTheSame(oldItem: HandsUpUser, newItem: HandsUpUser): Boolean {
            return oldItem == newItem && oldItem.userUuid == newItem.userUuid
        }

        override fun areContentsTheSame(oldItem: HandsUpUser, newItem: HandsUpUser): Boolean {
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
                && oldItem.isCoHost == newItem.isCoHost
        }
    }

    private abstract inner class BaseUserHolder(
        val view: View, val listener: UserItemClickListener
    ) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: HandsUpUser)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener) : ListAdapter<HandsUpUser, BaseUserHolder>(UserListDiff()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            createItemViewHolder(parent, listener)

        override fun onBindViewHolder(holder: BaseUserHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private interface UserItemClickListener {
        fun onUserCoHostStateChanged(item: HandsUpUser, isCoHost: Boolean)

    }


    private inner class ClassUserHolder(
        view: View,

        listener: UserItemClickListener
    ) : BaseUserHolder(view, listener) {

        private val name: TextView? = view.findViewById(R.id.tv_waving_name)
        private val desktopIcon: AppCompatImageView = view.findViewById(R.id.roster_item_desktop_icon)
        override fun bind(item: HandsUpUser) {
            name?.text = item.userName
            desktopIcon.isEnabled = item.isCoHost
            view.findViewById<ViewGroup>(R.id.roster_item_desktop_touch_area)?.let {
                it.setOnClickListener {
                    if (!item.isCoHost) {//还没上台前，点击后可上台
                        listener.onUserCoHostStateChanged(item, !item.isCoHost)
                        desktopIcon.isEnabled = !item.isCoHost
                    }
                }
            }
        }
    }
}
