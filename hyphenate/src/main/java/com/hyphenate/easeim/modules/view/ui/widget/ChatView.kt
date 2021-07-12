package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.EMError
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.view.`interface`.EaseOperationListener
import com.hyphenate.easeim.modules.view.`interface`.MessageListItemClickListener
import com.hyphenate.easeim.modules.view.`interface`.ViewClickListener
import com.hyphenate.easeim.modules.view.adapter.MessageAdapter
import com.hyphenate.util.EMLog

/**
 * 聊天页
 */
class ChatView(private val chatRoomId: String, context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EaseOperationListener, View.OnClickListener {
    constructor(chatRoomId: String, context: Context) : this(chatRoomId, context, null)
    constructor(chatRoomId: String, context: Context, attributeSet: AttributeSet?) : this(chatRoomId, context, attributeSet, 0)

    //伴生对象
    companion object {
        private const val TAG = "ChatFragment"
    }

    private lateinit var recyclerView: RecyclerView
    private val easeRepository = EaseRepository.instance

    private lateinit var inputMsgView: LinearLayout
    private val adapter = MessageAdapter()
    private lateinit var announcementView: LinearLayout
    private lateinit var announcementContent: TextView
    private lateinit var fragmentView: RelativeLayout
    private lateinit var defaultLayout: RelativeLayout
    private lateinit var tvContent: TextView
    private lateinit var faceView: FrameLayout
    private lateinit var faceIcon: ImageView

    var viewClickListener: ViewClickListener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.chat_view, this)
        initView()
    }

    /**
     * 初始化view
     */
    private fun initView() {
        fragmentView = findViewById(R.id.fragment_view)
        inputMsgView = findViewById(R.id.input_view)
        announcementView = findViewById(R.id.announcement_view)
        announcementContent = findViewById(R.id.tv_announcement)
        defaultLayout = findViewById(R.id.default_layout)
        tvContent = findViewById(R.id.tv_content)
        faceView = findViewById(R.id.face_view)
        faceIcon = findViewById(R.id.iv_face)
        recyclerView = findViewById(R.id.rv_list)
        val layoutManager = LinearLayoutManager(context.applicationContext)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        initListener()
        initData()
    }

    /**
     * 注册监听
     */
    private fun initListener() {
        easeRepository.addOperationListener(this)
        adapter.setMessageListItemClickListener(object : MessageListItemClickListener {
            override fun onResendClick(message: EMMessage): Boolean {
                EMLog.e(TAG, "onResendClick")
                message.setStatus(EMMessage.Status.CREATE)
                EMClient.getInstance().chatManager().sendMessage(message)
                refresh()
                return true
            }

            override fun onMessageError(message: EMMessage, code: Int, error: String?) {
                if (code == EMError.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.message_incloud_illegal_content), Toast.LENGTH_SHORT).show()
                    }
                }
                EMLog.e(TAG, "onMessageError:$code = $error")
            }

        })
        announcementContent.setOnClickListener {
            viewClickListener?.onAnnouncementClick()
        }
        tvContent.setOnClickListener(this)
        faceView.setOnClickListener(this)
    }

    /**
     * 初始化数据
     */
    private fun initData() {
        easeRepository.loadHistoryMessages(chatRoomId)
        easeRepository.fetchAnnouncement(chatRoomId)
        easeRepository.fetchChatRoomMutedStatus(chatRoomId)
    }

    /**
     * 刷新聊天列表
     */
    fun refresh() {
        easeRepository.loadMessages(chatRoomId)
    }

    fun announcementChange(announcement: String) {
        if (announcement.isNotEmpty()) {
            announcementView.visibility = VISIBLE
            announcementContent.text = announcement.replace("\n", " ")
        } else {
            announcementView.visibility = GONE
        }
    }

    override fun loadMessageFinish(messages: List<EMMessage>) {
        if (messages.isNotEmpty()) {
            defaultLayout.visibility = GONE
            adapter.setData(messages)
            recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
        } else {
            defaultLayout.visibility = VISIBLE
        }
    }

    override fun loadHistoryMessageFinish() {
        refresh()
    }

    override fun fetchAnnouncementFinish(announcement: String) {
        announcementChange(announcement)
    }

    override fun fetchChatRoomMutedStatus(isMuted: Boolean) {
        if (isMuted)
            showMutedView()
        else
            hideMutedView()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_content -> {
                viewClickListener?.onMsgContentClick()
            }
            R.id.face_view -> {
                viewClickListener?.onFaceIconClick()
            }
        }
    }

    /**
     * 显示禁言UI
     */
    fun showMutedView() {
        tvContent.hint = context.getString(R.string.muted)
        tvContent.isClickable = false
        faceView.isClickable = false
        faceIcon.visibility = INVISIBLE
    }

    /**
     * 隐藏禁言UI
     */
    fun hideMutedView() {
        tvContent.hint = context.getString(R.string.enter_contents)
        tvContent.isClickable = true
        faceView.isClickable = true
        faceIcon.visibility = VISIBLE
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) {
            handler.postDelayed({
                refresh()
            }, 300)

        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        easeRepository.removeOperationListener(this)
    }
}