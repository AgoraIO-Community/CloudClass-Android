package io.agora.online.easeim.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.agora.online.easeim.utils.CommonUtil
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.internal.util.AppUtil
import io.agora.online.R
import io.agora.online.provider.AgoraUIUserDetailInfo


class UserSearchView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) :
    LinearLayout(context, attributeSet, defStyleAttr) {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    var mUserSearchResultCallback: SearchResultCallback? = null

    var mUserSearchListRecyclerView: RecyclerView? = null
    var mUerEmpty: View? = null
    var mUserSearchListAdapter: SearchUserListAdapter? = null

    var mUserSearchCriteriaText: EditText? = null
    var mUserSearchGoButton: TextView? = null
    var mUserSearchCancelButton: ImageView? = null



    companion object {
        private const val TAG = "UserSearchView"
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.fcr_online_user_search_layout, this)

        mUserSearchListRecyclerView = findViewById(R.id.user_search_recycler_view)
        mUerEmpty = findViewById(R.id.fcr_search_empty)

        mUserSearchCriteriaText = findViewById(R.id.agora_search_criteria_text)
        mUserSearchGoButton = findViewById(R.id.agora_search_go_btn)
        mUserSearchCancelButton = findViewById(R.id.agora_search_cancel_btn)

        mUserSearchCriteriaText?.doAfterTextChanged { text ->
            if (text.isNullOrEmpty()) {
                mUserSearchListAdapter?.filter(null)
            }
        }

        mUserSearchCriteriaText?.setOnKeyListener { view, keyCode, event ->
            return@setOnKeyListener if (keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            ) {
                CommonUtil.hideSoftKeyboard(mUserSearchCriteriaText as EditText)
                mUserSearchListAdapter?.filter(mUserSearchCriteriaText?.text.toString())
                true
            } else false
        }

        mUserSearchGoButton?.setOnClickListener { view ->
            CommonUtil.hideSoftKeyboard(mUserSearchCriteriaText as EditText)
            mUserSearchListAdapter?.filter(mUserSearchCriteriaText?.text.toString())

            if (mUserSearchListAdapter?.filteredUserList?.isNotEmpty() == true) {
                mUserSearchListRecyclerView?.visibility = View.VISIBLE
                mUerEmpty?.visibility = View.GONE
            } else {
                mUserSearchListRecyclerView?.visibility = View.GONE
                mUerEmpty?.visibility = View.VISIBLE
            }
        }

        mUserSearchCancelButton?.setOnClickListener { view ->
            if (mUserSearchCriteriaText?.text.isNullOrEmpty()) {
                mUserSearchResultCallback?.onResultCancel()
            } else {
                mUserSearchCriteriaText?.text = null
            }
        }
    }

    fun initView(
        eduCore: AgoraEduCore,
        user: AgoraUIUserDetailInfo?,
        callback: SearchResultCallback
    ) {
        post {
            val allUser = eduCore.eduContextPool().userContext()?.getAllUserList()
            val userUuid = eduCore.eduContextPool().userContext()?.getLocalUserInfo()?.userUuid
            val allUserList = mutableListOf<AgoraEduContextUserInfo>()
            allUserList.add(AgoraEduContextUserInfo(CommonUtil.CHAT_ALL_USER_ID, CommonUtil.CHAT_ALL_USER_NAME))
            allUser?.forEach {
                if (it != null && it.userUuid != userUuid) {
                    allUserList.add(it)
                }
            }

            mUserSearchListAdapter = SearchUserListAdapter(context, callback, allUserList, user)
            mUserSearchListRecyclerView?.adapter = mUserSearchListAdapter;
            mUserSearchListAdapter?.notifyDataSetChanged()

            if (allUserList.isEmpty()) {
                mUserSearchListRecyclerView?.visibility = View.GONE
                mUerEmpty?.visibility = View.VISIBLE
            } else {
                mUserSearchListRecyclerView?.visibility = View.VISIBLE
                mUerEmpty?.visibility = View.GONE
            }
        }
        mUserSearchResultCallback = callback
    }

    class SearchUserItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumb: ImageView = itemView.findViewById(R.id.user_acc_thumb)
        val name: TextView = itemView.findViewById(R.id.user_acc_name)
        val action: TextView = itemView.findViewById(R.id.user_action_private)
        val lineView: View = itemView.findViewById(R.id.user_line)
        val privateNow: View = itemView.findViewById(R.id.user_private_now)
    }

    class SearchUserListAdapter(
        private val context: Context,
        private val callback: SearchResultCallback,
        private val allUserList: List<AgoraEduContextUserInfo>,
        private val current: AgoraUIUserDetailInfo?
    ) :
        RecyclerView.Adapter<SearchUserItemViewHolder>() {
        var filteredUserList = mutableListOf<AgoraEduContextUserInfo>()

        init {
            filteredUserList.addAll(allUserList)
        }

        fun filter(filter: String?) {
            val temp = if (filter == null || filter.isNullOrEmpty()) {
                allUserList
            } else {
                allUserList.filter { it.userName.contains(filter) }
            }
            filteredUserList.clear()
            filteredUserList.addAll(temp)

            notifyDataSetChanged()
            return
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): SearchUserItemViewHolder {
            val holder = SearchUserItemViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.fcr_online_user_search_list_item, parent, false)
            )
            holder.action.setOnClickListener { view: View ->
                val position = holder.absoluteAdapterPosition
                val user = filteredUserList[position]
                callback.onUserSelected(user)
            }
            return holder
        }

        override fun onBindViewHolder(holder: SearchUserItemViewHolder, position: Int) {
            val position = holder.absoluteAdapterPosition
            val user = filteredUserList[position]

            val bitmap =
                io.agora.online.util.AppUtil.generateRoundBitmap(
                    context,
                    io.agora.online.util.AppUtil.extractInitials(user.userName),
                    io.agora.online.util.AppUtil.dp2px(24).toFloat()
                )
            holder.thumb.setImageBitmap(bitmap)
            holder.name.text = user.userName

            // TODO(Hai_Guo) Is it best practice to hard code it here?
            // Do we need to query from somewhere?
            val md5Uuid = AppUtil.toMD5String(user.userUuid)
            // val properties: Map<String, Any>? = eduCore.eduContextPool().userContext()?.getUserProperties(user!!.userUuid)

            if (md5Uuid == current?.userUuid) {
                holder.action.visibility = View.GONE
                holder.privateNow.visibility = View.VISIBLE
            } else {
                holder.action.visibility = View.VISIBLE
                holder.privateNow.visibility = View.GONE
            }

            if (position == filteredUserList.size - 1) {
                holder.lineView.visibility = View.GONE
            } else {
                holder.lineView.visibility = View.VISIBLE
            }

            if (CommonUtil.CHAT_ALL_USER_ID == user.userUuid) { // 群聊
                Glide.with(holder.lineView.context).load(CommonUtil.AVATAR_URL).into(holder.thumb)
                holder.action.visibility = View.GONE
                holder.privateNow.visibility = View.GONE
                holder.itemView.setOnClickListener {
                    callback.onUserSelected(null)
                }
            } else {
                holder.itemView.setOnClickListener(null)
            }
        }

        override fun getItemCount(): Int {
            return filteredUserList.size
        }

    }

    interface SearchResultCallback {
        fun onUserSelected(user: AgoraEduContextUserInfo?)

        fun onResultCancel()
    }

}