package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
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
class ChatView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EaseOperationListener, View.OnClickListener {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

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
    private lateinit var messageFloat: LinearLayout
    private lateinit var prompt: AppCompatTextView
    private var inputContent = ""
    var chatRoomId = ""
    var messageCount = 0
    var newMsgCount = 0

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
        messageFloat = findViewById(R.id.message_float)
        prompt = findViewById(R.id.message_prompt)
        recyclerView = findViewById(R.id.rv_list)
        val layoutManager = LinearLayoutManager(context.applicationContext)
        recyclerView.layoutManager = layoutManager
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        initListener()
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
        announcementView.setOnClickListener {
            viewClickListener?.onAnnouncementClick()
        }

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    messageFloat.visibility = GONE
                    messageCount += newMsgCount
                    newMsgCount = 0
                }
            }
        })

        tvContent.setOnClickListener(this)
        faceView.setOnClickListener(this)
        messageFloat.setOnClickListener(this)
    }

    /**
     * 初始化数据
     */
    fun initData() {
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
            if ("\n" in announcement)
                announcementContent.text = announcement.split("\n")[0]
            else
                announcementContent.text = announcement
        } else {
            announcementView.visibility = GONE
        }
    }

    override fun loadMessageFinish(messages: List<EMMessage>) {
        if (messages.isNotEmpty()) {
            newMsgCount = messages.size - messageCount
            defaultLayout.visibility = GONE
            if (recyclerView.canScrollVertically(1) && messages[messages.size - 1].from != EMClient.getInstance().currentUser) {
                adapter.setData(messages)
                messageFloat.visibility = VISIBLE
                prompt.text = String.format(context.getString(R.string.new_message), newMsgCount.toString())
            } else {
                messageFloat.visibility = GONE
                messageCount = messages.size
                newMsgCount = 0
                adapter.setData(messages)
                recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
            }
        } else {
            defaultLayout.visibility = VISIBLE
            messageFloat.visibility = GONE
            messageCount = 0
            newMsgCount = 0
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
            R.id.message_float -> {
                messageFloat.visibility = GONE
                recyclerView.smoothScrollToPosition(adapter.itemCount - 1)
                messageCount += newMsgCount
                newMsgCount = 0
            }
        }
    }

    /**
     * 显示禁言UI
     */
    fun showMutedView() {
        if (EaseRepository.instance.allMuted) {
            tvContent.hint = context.getString(R.string.all_muted)
        } else if (EaseRepository.instance.singleMuted) {
            tvContent.hint = context.getString(R.string.single_muted)
        }
        tvContent.isClickable = false
        faceView.isClickable = false
        faceIcon.visibility = INVISIBLE
    }

    /**
     * 隐藏禁言UI
     */
    fun hideMutedView() {
        tvContent.hint = context.getString(R.string.enter_contents)
        if (inputContent.isNotEmpty()) {
            tvContent.hint = inputContent
        }
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

    fun setInputContent(content: String) {
        inputContent = content
        if (!EaseRepository.instance.singleMuted || EaseRepository.instance.allMuted) {
            hideMutedView()
        }
    }
}