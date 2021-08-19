package io.agora.uikit.impl.users

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.*
import io.agora.educontext.*
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.container.AgoraUIConfig
import io.agora.uikit.util.TextPinyinUtil
import kotlin.math.min

class AgoraUIRosterPopUp : RelativeLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attr: AttributeSet) : super(context, attr)
    constructor(context: Context, attr: AttributeSet, defStyleAttr: Int) : super(context, attr, defStyleAttr)

    private val tag = "AgoraUIRosterPopUp"
    private var userList: MutableList<EduContextUserDetailInfo> = mutableListOf()

    private var parent: ViewGroup? = null
    private var contentWidth = 0
    private var contentHeight = 0
    private var marginRight = 0
    private var marginBottom = 0

    private var recyclerView: RecyclerView? = null
    private var userListAdapter: UserListAdapter? = null
    private var tvTeacherName: TextView? = null

    private var eduContext: EduContextPool? = null
    private var rosterType = RosterType.SmallClass

    var closeRunnable: Runnable? = null

    private val handler = object : UserHandler() {
        override fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onUserListUpdated(list)
            val list1 = sort(list)
            updateUserList(list1)
            updateUserListAdapter(userList)
        }

        override fun onFlexUserPropsChanged(changedProperties: MutableMap<String, Any>, properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?, fromUser: EduContextUserDetailInfo, operator: EduContextUserInfo?) {
            super.onFlexUserPropsChanged(changedProperties, properties, cause, fromUser, operator)
            Log.i(tag, "onFlexUserPropertiesChanged")
        }
    }

    fun setEduContext(eduContextPool: EduContextPool?) {
        this.eduContext = eduContextPool
        this.eduContext?.userContext()?.addHandler(handler)
    }

    fun initView(parent: ViewGroup, right: Int, bottom: Int) {
        this.parent = parent
        marginRight = right
        marginBottom = bottom
    }

    fun setType(type: RosterType) {
        this.rosterType = type
    }

    fun show() {
        this.parent?.let { parent ->
            LayoutInflater.from(parent.context).inflate(getLayoutRes(this.rosterType), this)
            parent.addView(this)
            val param = this.layoutParams as MarginLayoutParams
            contentWidth = getLayoutWidth(rosterType)
            param.width = contentWidth
            contentHeight = getLayoutHeight(rosterType)
            param.height = contentHeight
            param.rightMargin = marginRight
            param.bottomMargin = marginBottom
            param.leftMargin = parent.width - marginRight - contentWidth
            param.topMargin = parent.height - marginBottom - contentHeight
            this.layoutParams = param

            recyclerView = findViewById(R.id.recycler_view)
            tvTeacherName = findViewById(R.id.tv_teacher_name)
            findViewById<View>(R.id.iv_close).setOnClickListener {
                dismiss()
                closeRunnable?.run()
            }

            userListAdapter = UserListAdapter(object : UserItemClickListener {
                override fun onCameraCheckChanged(item: EduContextUserDetailInfo, checked: Boolean) {
                    eduContext?.userContext()?.muteVideo(!checked)
                }

                override fun onMicCheckChanged(item: EduContextUserDetailInfo, checked: Boolean) {
                    eduContext?.userContext()?.muteAudio(!checked)
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
            val studentList = filterStudent(userList)
            userListAdapter?.submitList(ArrayList(studentList))
        }
    }

    private fun getLayoutRes(type: RosterType) = when (type) {
        RosterType.SmallClass -> R.layout.agora_userlist_dialog_layout
        RosterType.LargeClass -> R.layout.agora_userlist_largeclass_dialog_layout
    }

    private fun getLayoutWidth(type: RosterType): Int = resources.getDimensionPixelSize(
        when (type) {
            RosterType.SmallClass -> R.dimen.agora_userlist_dialog_width
            RosterType.LargeClass -> R.dimen.agora_userlist_largeclass_dialog_width
        })

    private fun getLayoutHeight(type: RosterType): Int = resources.getDimensionPixelSize(
        when (type) {
            RosterType.SmallClass -> R.dimen.agora_userlist_dialog_height
            RosterType.LargeClass -> R.dimen.agora_userlist_dialog_height
        }
    )

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

    fun sort(list: MutableList<EduContextUserDetailInfo>): MutableList<EduContextUserDetailInfo> {
        var coHosts = mutableListOf<EduContextUserDetailInfo>()
        val users = mutableListOf<EduContextUserDetailInfo>()
        list.forEach {
            if (it.coHost) {
                coHosts.add(it)
            } else {
                users.add(it)
            }
        }
        coHosts = sort2(coHosts)
        val list1 = sort2(users)
        coHosts.addAll(list1)
        return coHosts
    }

    private fun sort2(list: MutableList<EduContextUserDetailInfo>): MutableList<EduContextUserDetailInfo> {
        val numList = mutableListOf<EduContextUserDetailInfo>()
        val listIterator = list.iterator()
        while (listIterator.hasNext()) {
            val info = listIterator.next()
            val tmp = info.user.userName[0]
            if (!TextPinyinUtil.isChinaString(tmp.toString()) && tmp.toInt() in 48..57) {
                numList.add(info)
                listIterator.remove()
            }
        }
        numList.sortWith(object : Comparator<EduContextUserDetailInfo> {
            override fun compare(o1: EduContextUserDetailInfo?, o2: EduContextUserDetailInfo?): Int {
                if (o1 == null) {
                    return -1
                }
                if (o2 == null) {
                    return 1
                }
                return o1.user.userName.compareTo(o2.user.userName)
            }
        })
        list.sortWith(object : Comparator<EduContextUserDetailInfo> {
            override fun compare(o1: EduContextUserDetailInfo?, o2: EduContextUserDetailInfo?): Int {
                if (o1 == null) {
                    return -1
                }
                if (o2 == null) {
                    return 1
                }
                var ch1 = ""
                if (TextPinyinUtil.isChinaString(o1.user.userName)) {
                    TextPinyinUtil.getPinyin(o1.user.userName).let {
                        ch1 = it
                    }
                } else {
                    ch1 = o1.user.userName
                }
                var ch2 = ""
                if (TextPinyinUtil.isChinaString(o2.user.userName)) {
                    TextPinyinUtil.getPinyin(o2.user.userName).let {
                        ch2 = it
                    }
                } else {
                    ch2 = o2.user.userName
                }
                return ch1.compareTo(ch2)
            }
        })
        list.addAll(numList)
        return list
    }

    private fun updateUserList(list: MutableList<EduContextUserDetailInfo>) {
        userList.clear()
        list.forEach {
            userList.add(it.copy())
        }
    }

    fun updateUserListAdapter(list: MutableList<EduContextUserDetailInfo>) {
        val studentList = filterStudent(list)
        userListAdapter?.submitList(ArrayList(studentList))
    }

    private fun filterStudent(list: MutableList<EduContextUserDetailInfo>): MutableList<EduContextUserDetailInfo> {
        val studentList = mutableListOf<EduContextUserDetailInfo>()
        list.forEach { item ->
            if (item.user.role == EduContextUserRole.Student) {
                studentList.add(item)
            } else if (item.user.role == EduContextUserRole.Teacher) {
                updateTeacher(item)
            }
        }
        return studentList
    }

    private fun updateTeacher(info: EduContextUserDetailInfo) {
        tvTeacherName?.post { tvTeacherName?.text = info.user.userName }
    }

    private fun updateStudent(info: EduContextUserDetailInfo) {
        val index = findIndex(info)
        if (index >= 0) {
            userListAdapter?.currentList?.set(index, info)
            userListAdapter?.notifyItemChanged(index)
        }
    }

    private fun findIndex(info: EduContextUserDetailInfo): Int {
        var index = 0
        var foundIndex = -1;
        for (item in userListAdapter?.currentList!!) {
            if (item.user.userUuid == info.user.userUuid) {
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
                                     listener: UserItemClickListener): BaseUserHolder {
        return when (type) {
            RosterType.SmallClass -> {
                SmallClassUserHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.agora_userlist_dialog_list_item, null), listener)
            }
            RosterType.LargeClass -> {
                LargeClassUserHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.agora_userlist_largeclass_dialog_list_item, null), listener)
            }
        }
    }

    private class UserListDiff : DiffUtil.ItemCallback<EduContextUserDetailInfo>() {
        override fun areItemsTheSame(oldItem: EduContextUserDetailInfo, newItem: EduContextUserDetailInfo): Boolean {
            return oldItem == newItem && oldItem.user.userUuid == newItem.user.userUuid
        }

        override fun areContentsTheSame(oldItem: EduContextUserDetailInfo, newItem: EduContextUserDetailInfo): Boolean {
            return oldItem.user.userName == newItem.user.userName
                    && oldItem.onLine == newItem.onLine
                    && oldItem.coHost == newItem.coHost
                    && oldItem.boardGranted == newItem.boardGranted
                    && oldItem.cameraState == newItem.cameraState
                    && oldItem.microState == newItem.microState
                    && oldItem.enableAudio == newItem.enableAudio
                    && oldItem.enableVideo == newItem.enableVideo
                    && oldItem.rewardCount == newItem.rewardCount
        }
    }

    private abstract inner class BaseUserHolder(
        private val type: RosterType,
        val view: View, val listener: UserItemClickListener
    ) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: EduContextUserDetailInfo)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener)
        : ListAdapter<EduContextUserDetailInfo, BaseUserHolder>(UserListDiff()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            createItemViewHolder(rosterType, parent, listener)

        override fun onBindViewHolder(holder: BaseUserHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private interface UserItemClickListener {
        fun onCameraCheckChanged(item: EduContextUserDetailInfo, checked: Boolean)
        fun onMicCheckChanged(item: EduContextUserDetailInfo, checked: Boolean)
    }

    private inner class SmallClassUserHolder(
        view: View, listener: UserItemClickListener) : BaseUserHolder(rosterType, view, listener) {

        private val tvName: TextView? = view.findViewById(R.id.tv_user_name)
        private val ctvDesktop: CheckedTextView? = view.findViewById(R.id.ctv_desktop)
        private val ctvAccess: CheckedTextView? = view.findViewById(R.id.ctv_access)
        private val ctvCamera: CheckedTextView? = view.findViewById(R.id.ctv_camera)
        private val ctvMic: CheckedTextView? = view.findViewById(R.id.ctv_mic)
        private val ctvSilence: CheckedTextView? = view.findViewById(R.id.ctv_silence)
        private val ctvStar: CheckedTextView? = view.findViewById(R.id.ctv_star)

        override fun bind(item: EduContextUserDetailInfo) {
            tvName?.text = item.user.userName
            ctvDesktop?.isEnabled = item.coHost
            ctvAccess?.isEnabled = item.boardGranted

            ctvCamera?.let { camera ->
                if (item.cameraState == EduContextDeviceState.Closed) {
                    camera.isEnabled = false
                    camera.isChecked = false
                    return@let
                }
                if (item.coHost) {
                    camera.isEnabled = item.isSelf
                } else {
                    camera.isEnabled = false
                }
                camera.isChecked = item.enableVideo
                camera.setOnClickListener {
                    camera.isClickable = false
                    camera.isChecked = !camera.isChecked
                    listener.onCameraCheckChanged(item, camera.isChecked)
                    camera.postDelayed({ camera.isClickable = true }, AgoraUIConfig.clickInterval)
                }
            }

            ctvMic?.let { mic ->
                if (item.microState == EduContextDeviceState.Closed) {
                    mic.isEnabled = false
                    mic.isChecked = false
                    return@let
                }
                if (item.coHost) {
                    mic.isEnabled = item.isSelf
                } else {
                    mic.isEnabled = false
                }

                mic.isChecked = item.enableAudio
                mic.setOnClickListener {
                    mic.isClickable = false
                    mic.isChecked = !mic.isChecked
                    listener.onMicCheckChanged(item, mic.isChecked)
                    mic.postDelayed({ mic.isClickable = true }, AgoraUIConfig.clickInterval)
                }
            }

            ctvSilence?.isEnabled = item.silence

            val tmp = min(item.rewardCount, 99)
            ctvStar?.text = view.resources.getString(R.string.agora_video_reward, tmp)
        }
    }

    private inner class LargeClassUserHolder(
        view: View, listener: UserItemClickListener) : BaseUserHolder(rosterType, view, listener) {
        private val tvName: TextView? = view.findViewById(R.id.tv_user_name)
        private val ctvCamera: CheckedTextView? = view.findViewById(R.id.ctv_camera)
        private val ctvMic: CheckedTextView? = view.findViewById(R.id.ctv_mic)

        override fun bind(item: EduContextUserDetailInfo) {
            tvName?.let { nameTextView ->
                val nameStr = SpannableString(item.user.userName.plus(" "))

                if (item.coHost) {
                    nameStr.setSpan(
                        ImageSpan(ContextCompat.getDrawable(view.context,
                        R.drawable.agora_userlist_desktop_icon)!!.apply {
                        setBounds(10, 0, intrinsicWidth + 10, intrinsicHeight)
                    },
                        ImageSpan.ALIGN_BASELINE),
                        nameStr.length - 1,
                        nameStr.length,
                        SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
                }

                nameTextView.text = nameStr
            }

            ctvCamera?.let { camera ->
                if (item.cameraState == EduContextDeviceState.Closed) {
                    camera.isEnabled = false
                    camera.isChecked = false
                    return@let
                }
                if (item.coHost) {
                    camera.isEnabled = item.isSelf
                } else {
                    camera.isEnabled = false
                }

                camera.isChecked = item.enableVideo
                camera.setOnClickListener {
                    camera.isClickable = false
                    camera.isChecked = !camera.isChecked
                    listener.onCameraCheckChanged(item, camera.isChecked)
                    camera.postDelayed({ camera.isClickable = true }, AgoraUIConfig.clickInterval)
                }
            }

            ctvMic?.let { mic ->
                if (item.microState == EduContextDeviceState.Closed) {
                    mic.isEnabled = false
                    mic.isChecked = false
                    return@let
                }
                if (item.coHost) {
                    mic.isEnabled = item.isSelf
                } else {
                    mic.isEnabled = false
                }

                mic.isChecked = item.enableAudio
                mic.setOnClickListener {
                    mic.isClickable = false
                    mic.isChecked = !mic.isChecked
                    listener.onMicCheckChanged(item, mic.isChecked)
                    mic.postDelayed({ mic.isClickable = true }, AgoraUIConfig.clickInterval)
                }
            }
        }
    }
}