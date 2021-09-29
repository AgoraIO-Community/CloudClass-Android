package io.agora.agoraeduuikit.impl.privatechat

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeducontext.EduContextPrivateChatInfo
import io.agora.agoraeducontext.EduContextUserDetailInfo
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.AbsComponent

@SuppressLint("InflateParams")
class PrivateChatList(val parent: ViewGroup,
                      val width: Int,
                      val height: Int,
                      val left: Int,
                      val top: Int,
                    val eduContext: io.agora.agoraeducore.core.context.EduContextPool?) : AbsComponent() {
    private val recycler: RecyclerView
    private val adapter: PrivateChatListAdapter

    private var localUserId: String? = null
    private var localUserName: String? = null

    init {
        val layout = LayoutInflater.from(parent.context).inflate(
                R.layout.agora_private_chat_layout, null, false)
        parent.addView(layout)
        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
        params.width = width
        params.height = height
        params.topMargin = top
        params.leftMargin = left
        layout.layoutParams = params

        recycler = layout.findViewById(R.id.agora_private_chat_user_list)
        recycler.layoutManager = LinearLayoutManager(parent.context,
                LinearLayoutManager.VERTICAL, false)
        adapter = PrivateChatListAdapter()
        recycler.adapter = adapter
    }

    override fun setRect(rect: Rect) {

    }

    fun setLocalUserInfo(userId: String, userName: String) {
        this.localUserId = userId
        this.localUserName = userName
    }

    fun updatePrivateChatUserList(list: MutableList<EduContextUserDetailInfo>) {
        recycler.post { adapter.updateUserInfo(list) }
    }

    @Synchronized fun setPrivateChatState(started: Boolean, info: EduContextPrivateChatInfo?) {
        recycler.post {
            info?.let { privateInfo ->
                adapter.setPrivateChatState(started, privateInfo)
            }
        }
    }

    inner class PrivateChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: AppCompatTextView = itemView.findViewById(R.id.agora_private_chat_item_name)
        val button: AppCompatButton = itemView.findViewById(R.id.agora_private_chat_item_button)
        val text: AppCompatTextView = itemView.findViewById(R.id.agora_private_chat_item_text)
    }

    inner class PrivateChatListAdapter: RecyclerView.Adapter<PrivateChatViewHolder>() {
        val list = mutableListOf<EduContextUserDetailInfo>()

        var privateChatStarted = false
        var privateChatInfo: EduContextPrivateChatInfo? = null

        fun updateUserInfo(list: MutableList<EduContextUserDetailInfo>) {
            this.list.clear()
            this.list.addAll(list)
            notifyDataSetChanged()
        }

        @SuppressLint("InflateParams")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrivateChatViewHolder {
            return PrivateChatViewHolder(LayoutInflater.from(parent.context)
                    .inflate(R.layout.agora_private_chat_item_layout, null, false))
        }

        override fun onBindViewHolder(holder: PrivateChatViewHolder, position: Int) {
            val pos = holder.adapterPosition
            holder.name.text = list[pos].user.userName
            holder.button.visibility = View.INVISIBLE
            holder.text.visibility = View.INVISIBLE

            if (privateChatStarted && privateChatInfo != null) {
                if (privateChatInfo!!.toUser.userUuid == list[pos].user.userUuid ||
                        privateChatInfo!!.fromUser.userUuid == list[pos].user.userUuid) {
                    holder.text.visibility = View.VISIBLE
                    holder.text.text = "语音中"
                }

                if ((privateChatInfo!!.toUser.userUuid == list[pos].user.userUuid ||
                        privateChatInfo!!.fromUser.userUuid == list[pos].user.userUuid) &&
                        (privateChatInfo!!.toUser.userUuid == localUserId ||
                                privateChatInfo!!.fromUser.userUuid == localUserId)) {
                    if (localUserId != list[pos].user.userUuid) {
                        holder.button.visibility = View.VISIBLE
                        holder.button.text = "结束语音"
                        holder.button.setOnClickListener {
                            eduContext?.privateChatContext()?.endPrivateChat(null)
                        }
                    }
                }
            } else {
                var contains = false
                list.forEach {
                    if (it.user.userUuid == localUserId) {
                        contains = true
                    }
                }

                if (list[pos].user.userUuid != localUserId && contains) {
                    holder.button.visibility = View.VISIBLE
                    holder.button.text = "开始语音"
                    holder.button.setOnClickListener {
                        eduContext?.privateChatContext()?.startPrivateChat(list[pos].user.userUuid, null)
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        fun setPrivateChatState(started: Boolean, info: EduContextPrivateChatInfo?) {
            privateChatStarted = started
            this.privateChatInfo = info
            notifyDataSetChanged()
        }
    }
}