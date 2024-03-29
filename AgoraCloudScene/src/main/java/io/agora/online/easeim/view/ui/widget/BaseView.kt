package io.agora.online.easeim.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import io.agora.online.easeim.repositories.EaseRepository
import io.agora.online.easeim.view.`interface`.EaseOperationListener
import io.agora.chat.ChatMessage

abstract class BaseView (context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EaseOperationListener, View.OnClickListener {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    val easeRepository = EaseRepository.instance

    init {
        initLayout()
    }

    private fun initLayout(){
        setLayout()
        initView()
        initListener()
    }
    abstract fun setLayout()
    abstract fun initView()
    open fun initListener(){
        easeRepository.addOperationListener(this)
    }

    override fun onClick(v: View?) {

    }

    override fun loadHistoryMessageFinish() {

    }

    override fun loadMessageFinish(messages: List<ChatMessage>) {

    }

    override fun fetchAnnouncementFinish(announcement: String) {
        Log.e("announcementChange","baseView: "+announcement)
    }

    override fun fetchChatRoomMutedStatus(isMuted: Boolean) {

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        easeRepository.removeOperationListener(this)
    }
}