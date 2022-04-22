package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.EMError
import com.hyphenate.EMValueCallBack
import com.hyphenate.chat.EMChatRoom
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
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

    //伴生对象
    companion object {
        private const val TAG = "ChatFragment"
    }

    private lateinit var recyclerView: RecyclerView
    private val easeRepository = EaseRepository.instance

    private val adapter = MessageAdapter()
    private lateinit var announcementView: LinearLayout
    private lateinit var announcementContent: TextView
    private lateinit var fragmentView: RelativeLayout
    private lateinit var defaultLayout: RelativeLayout
    var tvInputView: RelativeLayout? = null
    private lateinit var tvContent: TextView
    private lateinit var faceIcon: ImageView
    private lateinit var picIcon: ImageView
    private lateinit var muteView: FrameLayout
    private lateinit var muteIcon: AppCompatImageView
    private lateinit var unMuteIcon: AppCompatImageView
    private var inputContent = ""

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
        announcementView = findViewById(R.id.announcement_view)
        announcementContent = findViewById(R.id.tv_announcement)
        defaultLayout = findViewById(R.id.default_layout)
        tvInputView = findViewById(R.id.input_view)
        tvContent = findViewById(R.id.tv_content)
        faceIcon = findViewById(R.id.iv_face)
        picIcon = findViewById(R.id.iv_picture)
        muteView = findViewById(R.id.mute_view)
        muteIcon = findViewById(R.id.iv_mute)
        unMuteIcon = findViewById(R.id.iv_unmute)
        recyclerView = findViewById(R.id.rv_list)
        val layoutManager = LinearLayoutManager(context.applicationContext)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        if(EaseRepository.instance.isStudentRole()){
            muteView.visibility = View.GONE
        }

        initListener()
    }

    /**
     * 注册监听
     */
    private fun initListener() {
        // memory leak
//        easeRepository.addOperationListener(this)
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

            override fun onItemClick(v: View, message: EMMessage) {
                if(message.type == EMMessage.Type.IMAGE){
                    viewClickListener?.onImageClick(message)
                }
            }
        })
        announcementView.setOnClickListener {
            viewClickListener?.onAnnouncementClick()
        }
        tvContent.setOnClickListener(this)
        faceIcon.setOnClickListener(this)
        picIcon.setOnClickListener(this)
        muteView.setOnClickListener(this)
    }

    /**
     * 初始化数据
     */
    fun initData() {
        easeRepository.loadHistoryMessages()
        easeRepository.fetchAnnouncement()
        easeRepository.fetchChatRoomMutedStatus()
    }

    /**
     * 刷新聊天列表
     */
    fun refresh() {
        easeRepository.loadMessages()
    }

    fun announcementChange(announcement: String) {
        if (announcement.isNotEmpty()) {
            announcementView.visibility = VISIBLE
            if("\n" in announcement)
                announcementContent.text = announcement.split("\n")[0]
            else
                announcementContent.text = announcement
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
        if(EaseRepository.instance.isStudentRole()) {
            if (isMuted)
                showMutedView()
            else
                hideMutedView()
        } else {
            changeMuteView()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_content -> {
                viewClickListener?.onMsgContentClick()
            }
            R.id.iv_face -> {
                viewClickListener?.onFaceIconClick()
            }
            R.id.iv_picture -> {
                viewClickListener?.onPicIconClick()
            }
            R.id.mute_view -> {
                if(EaseRepository.instance.isInit && EaseRepository.instance.isLogin){
                    muteView.isClickable = false
                    if (unMuteIcon.visibility == View.VISIBLE) {
                        EMClient.getInstance().chatroomManager().muteAllMembers(EaseRepository.instance.chatRoomId, object : EMValueCallBack<EMChatRoom> {
                            override fun onSuccess(value: EMChatRoom?) {
                                ThreadManager.instance.runOnMainThread {
                                    changeMuteIcon()
                                    muteView.isClickable = true
                                    EaseRepository.instance.sendOperationMessage(EaseConstant.SET_ALL_MUTE, "", "", null)
                                }
                            }

                            override fun onError(error: Int, errorMsg: String?) {
                                ThreadManager.instance.runOnMainThread {
                                    muteView.isClickable = true
                                    Toast.makeText(context, context.getString(R.string.global_mute_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    } else {
                        EMClient.getInstance().chatroomManager().unmuteAllMembers(EaseRepository.instance.chatRoomId, object : EMValueCallBack<EMChatRoom> {
                            override fun onSuccess(value: EMChatRoom?) {
                                ThreadManager.instance.runOnMainThread {
                                    changeMuteIcon()
                                    muteView.isClickable = true
                                    EaseRepository.instance.sendOperationMessage(EaseConstant.REMOVE_ALL_MUTE, "", "", null)
                                }
                            }

                            override fun onError(error: Int, errorMsg: String?) {
                                ThreadManager.instance.runOnMainThread {
                                    muteView.isClickable = true
                                    Toast.makeText(context, context.getString(R.string.global_remove_mute_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    /**
     * 显示禁言UI
     */
    fun showMutedView() {
        if(EaseRepository.instance.allMuted){
            tvContent.hint = context.getString(R.string.all_muted)
        }else if(EaseRepository.instance.singleMuted){
            tvContent.hint = context.getString(R.string.single_muted)
        }
        tvContent.isClickable = false
        faceIcon.isClickable = false
        picIcon.isClickable = false
    }

    /**
     * 隐藏禁言UI
     */
    fun hideMutedView() {
        tvContent.hint = context.getString(R.string.enter_contents)
        if(inputContent.isNotEmpty()){
            tvContent.hint = inputContent
        }
        tvContent.isClickable = true
        faceIcon.isClickable = true
        picIcon.isClickable = true
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        if (visibility == VISIBLE) {
            handler.postDelayed({
                refresh()
            }, 300)

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        easeRepository.addOperationListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        easeRepository.removeOperationListener(this)
    }

    fun setInputContent(content: String){
        inputContent = content
        if(!EaseRepository.instance.singleMuted || EaseRepository.instance.allMuted){
            hideMutedView()
        }
    }

    private fun changeMuteIcon(){
        if(unMuteIcon.visibility == View.VISIBLE){
            unMuteIcon.visibility = View.GONE
            muteIcon.visibility = View.VISIBLE
        } else {
            unMuteIcon.visibility = View.VISIBLE
            muteIcon.visibility = View.GONE
        }
    }

    private fun changeMuteView(){
        if(EaseRepository.instance.allMuted){
            unMuteIcon.visibility = View.GONE
            muteIcon.visibility = View.VISIBLE
        } else {
            unMuteIcon.visibility = View.VISIBLE
            muteIcon.visibility = View.GONE
        }
    }
}