package io.agora.uikit.impl.users

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Rect
import android.text.SpannableString
import android.text.style.ImageSpan
import android.view.*
import android.widget.CheckedTextView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import io.agora.educontext.EduContextDeviceState
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextUserDetailInfo
import io.agora.educontext.EduContextUserRole
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.impl.container.AgoraUIConfig.clickInterval
import io.agora.uikit.util.TextPinyinUtil
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.min

class AgoraUIRoster(private val eduContext: EduContextPool?) : AbsComponent() {
    private var rosterDialog: RosterDialog? = null
    private var userList: MutableList<EduContextUserDetailInfo> = mutableListOf()

    private val handler = object : UserHandler() {
        override fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onUserListUpdated(list)
            val list1 = sort(list)
            updateUserList(list1)
            rosterDialog?.updateUserList(userList)
        }

        override fun onRoster(context: Context, anchor: View, type: Int?) {
            when (type) {
                RosterType.SmallClass.value() -> RosterType.SmallClass
                RosterType.LargeClass.value() -> RosterType.LargeClass
                else -> null
            }?.let { rosterType ->
                dismiss()
                RosterDialog(context, rosterType, eduContext, userList).let { dialog ->
                    dialog.setOnDismissListener(dismissListener)
                    rosterDialog = dialog
                    showDialog(anchor)
                }
            }
        }
    }

    companion object {
        var dismissListener: DialogInterface.OnDismissListener? = null
    }

    fun sort(list: MutableList<EduContextUserDetailInfo>): MutableList<EduContextUserDetailInfo> {
        var coHosts = mutableListOf<EduContextUserDetailInfo>()
        var users = mutableListOf<EduContextUserDetailInfo>()
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

    private fun isShowing(): Boolean {
        return rosterDialog?.isShowing ?: false
    }

    private fun showDialog(anchor: View) {
        rosterDialog?.adjustPosition(anchor)
        rosterDialog?.show()
    }

    private fun dismiss() {
        if (isShowing()) {
            rosterDialog!!.setOnDismissListener(null)
            rosterDialog!!.dismiss()
            rosterDialog = null
        }
    }

    init {
        eduContext?.userContext()?.addHandler(handler)
    }

    enum class RosterType(private val value: Int) {
        SmallClass(0), LargeClass(1);

        fun value(): Int {
            return this.value
        }
    }

    override fun setRect(rect: Rect) {

    }
}

class RosterDialog(
        appContext: Context,
        private val type: AgoraUIRoster.RosterType,
        private val eduContext: EduContextPool?,
        private val userList: MutableList<EduContextUserDetailInfo>
) : Dialog(appContext, R.style.agora_dialog) {

    private val recyclerView: RecyclerView
    private val userListAdapter: UserListAdapter
    private val tvTeacherName: TextView

    init {
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        setContentView(getLayoutRes())
        recyclerView = findViewById(R.id.recycler_view)
        tvTeacherName = findViewById(R.id.tv_teacher_name)
        findViewById<View>(R.id.iv_close).setOnClickListener { dismiss() }

        userListAdapter = UserListAdapter(object : UserItemClickListener {
            override fun onCameraCheckChanged(item: EduContextUserDetailInfo, checked: Boolean) {
                eduContext?.userContext()?.muteVideo(!checked)
            }

            override fun onMicCheckChanged(item: EduContextUserDetailInfo, checked: Boolean) {
                eduContext?.userContext()?.muteAudio(!checked)
            }
        })

        recyclerView.addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(ContextCompat.getDrawable(context, R.drawable.agora_userlist_divider)!!)
                })

        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        recyclerView.itemAnimator?.addDuration = 0
        recyclerView.itemAnimator?.changeDuration = 0
        recyclerView.itemAnimator?.moveDuration = 0
        recyclerView.itemAnimator?.removeDuration = 0
        (recyclerView.itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false

        recyclerView.adapter = userListAdapter
        val studentList = filterStudent(userList)
        userListAdapter.submitList(ArrayList(studentList))
    }

    private fun getLayoutRes() = when (this.type) {
        AgoraUIRoster.RosterType.SmallClass -> R.layout.agora_userlist_dialog_layout
        AgoraUIRoster.RosterType.LargeClass -> R.layout.agora_userlist_largeclass_dialog_layout
    }

    fun adjustPosition(anchor: View) {
        when (type) {
            AgoraUIRoster.RosterType.SmallClass -> {
                adjustPosition(anchor,
                        context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_width),
                        context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_height))
            }

            AgoraUIRoster.RosterType.LargeClass -> {
                adjustPosition(anchor,
                        context.resources.getDimensionPixelSize(R.dimen.agora_userlist_largeclass_dialog_width),
                        context.resources.getDimensionPixelSize(R.dimen.agora_userlist_dialog_height))
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun createItemViewHolder(type: AgoraUIRoster.RosterType, parent: ViewGroup, listener: UserItemClickListener): BaseUserHolder {
        return when (type) {
            AgoraUIRoster.RosterType.SmallClass -> {
                SmallClassUserHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.agora_userlist_dialog_list_item, null), listener)
            }
            AgoraUIRoster.RosterType.LargeClass -> {
                LargeClassUserHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.agora_userlist_largeclass_dialog_list_item, null), listener)
            }
        }
    }

    private fun adjustPosition(anchor: View, width: Int, height: Int) {
        this.window?.let { window ->
            hideStatusBar(window)

            val params = window.attributes
            params.width = width
            params.height = height

            val posArray = IntArray(2)
            anchor.getLocationOnScreen(posArray)
            params.x = posArray[0] + anchor.width + 12
            params.gravity = Gravity.CENTER_VERTICAL or Gravity.LEFT
            window.attributes = params
        }
    }

    private fun hideStatusBar(window: Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.decorView.systemUiVisibility = (flag or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    fun updateUserList(list: MutableList<EduContextUserDetailInfo>) {
        val studentList = filterStudent(list)
        userListAdapter.submitList(ArrayList(studentList))
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
        tvTeacherName.post { tvTeacherName.text = info.user.userName }
    }

    private fun updateStudent(info: EduContextUserDetailInfo) {
        val index = findIndex(info)
        if (index >= 0) {
            userListAdapter.currentList[index] = info
            userListAdapter.notifyItemChanged(index)
        }
    }

    private fun findIndex(info: EduContextUserDetailInfo): Int {
        var index = 0
        var foundIndex = -1;
        for (item in userListAdapter.currentList) {
            if (item.user.userUuid == info.user.userUuid) {
                foundIndex = index
                break
            }
            index++
        }
        return foundIndex
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
            private val type: AgoraUIRoster.RosterType,
            val view: View, val listener: UserItemClickListener) : RecyclerView.ViewHolder(view) {
        abstract fun bind(item: EduContextUserDetailInfo)
    }

    private inner class UserListAdapter(val listener: UserItemClickListener)
        : ListAdapter<EduContextUserDetailInfo, BaseUserHolder>(UserListDiff()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                createItemViewHolder(type, parent, listener)

        override fun onBindViewHolder(holder: BaseUserHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    private interface UserItemClickListener {
        fun onCameraCheckChanged(item: EduContextUserDetailInfo, checked: Boolean)
        fun onMicCheckChanged(item: EduContextUserDetailInfo, checked: Boolean)
    }

    private inner class SmallClassUserHolder(view: View, listener: UserItemClickListener) : BaseUserHolder(type, view, listener) {
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
                    camera.postDelayed({ camera.isClickable = true }, clickInterval)
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
                    mic.postDelayed({ mic.isClickable = true }, clickInterval)
                }
            }

            ctvSilence?.isEnabled = item.silence

            val tmp = min(item.rewardCount, 99)
            ctvStar?.text = view.resources.getString(R.string.agora_video_reward, tmp)
        }
    }

    private inner class LargeClassUserHolder(view: View, listener: UserItemClickListener) : BaseUserHolder(type, view, listener) {
        private val tvName: TextView? = view.findViewById(R.id.tv_user_name)
        private val ctvCamera: CheckedTextView? = view.findViewById(R.id.ctv_camera)
        private val ctvMic: CheckedTextView? = view.findViewById(R.id.ctv_mic)

        override fun bind(item: EduContextUserDetailInfo) {
            tvName?.let { nameTextView ->
                val nameStr = SpannableString(item.user.userName)

                if (item.coHost) {
                    nameStr.setSpan(ImageSpan(ContextCompat.getDrawable(view.context,
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
                    camera.postDelayed({ camera.isClickable = true }, clickInterval)
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
                    mic.postDelayed({ mic.isClickable = true }, clickInterval)
                }
            }
        }
    }
}