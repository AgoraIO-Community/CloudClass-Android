package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.view.`interface`.EaseOperationListener

/**
 * 公告页
 */
class AnnouncementView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), EaseOperationListener {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    lateinit var content: TextView
    lateinit var defaultLayout: RelativeLayout
    private val easeRepository = EaseRepository.instance

    init {
        LayoutInflater.from(context).inflate(R.layout.announcement_view, this)
        initViews()
    }

    private fun initViews(){
        content = findViewById(R.id.announcement_content)
        defaultLayout = findViewById(R.id.default_layout)
        easeRepository.addOperationListener(this)
    }

    fun announcementChange(announcement: String){
        if (announcement.isNotEmpty()) {
            defaultLayout.visibility = GONE
            content.text = announcement
        } else {
            defaultLayout.visibility = VISIBLE
        }
    }

    override fun loadMessageFinish(messages: List<EMMessage>) {
        // 不需要处理
    }

    override fun loadHistoryMessageFinish() {
        // 不需要处理
    }

    override fun fetchAnnouncementFinish(announcement: String) {
        announcementChange(announcement)
    }

    override fun fetchChatRoomAllMutedStatus(allMuted: Boolean) {
        // 不需要处理
    }

    override fun fetchChatRoomSingleMutedStatus(isMuted: Boolean) {
        // 不需要处理
    }


}