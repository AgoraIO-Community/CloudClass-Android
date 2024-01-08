package com.hyphenate.easeim.modules.view.ui.widget

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.agoraeduuikit.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.repositories.EaseRepository
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.ViewEventListener
import io.agora.CallBack
import io.agora.chat.ChatClient
import io.agora.chat.ChatMessage

/**
 * 公告页
 */
class AnnouncementView(context: Context) : BaseView(context) {

    lateinit var content: TextView
    lateinit var defaultLayout: RelativeLayout
    lateinit var issueLayout: RelativeLayout
    lateinit var noticeRoot: FrameLayout
    lateinit var etNotice: AppCompatEditText
    lateinit var tvPrompt: AppCompatTextView
    lateinit var tvCount: AppCompatTextView
    lateinit var tvCancel: AppCompatTextView
    lateinit var tvEnter: AppCompatTextView
    lateinit var tvIssue: AppCompatTextView

    lateinit var permView: LinearLayout
    lateinit var ivUpdate: AppCompatImageView
    lateinit var ivRemove: AppCompatImageView

    var announcement = ""
    var viewEventListener: ViewEventListener? = null

    override fun setLayout() {
        LayoutInflater.from(context).inflate(R.layout.fcr_announcement_view, this)
    }

    override fun initView() {
        content = findViewById(R.id.announcement_content)
        defaultLayout = findViewById(R.id.default_layout)

        issueLayout = findViewById(R.id.issue_root)
        noticeRoot = findViewById(R.id.notice_root)
        etNotice = findViewById(R.id.et_notice)
        tvPrompt = findViewById(R.id.tv_prompt)
        tvCount = findViewById(R.id.tv_count)
        tvCancel = findViewById(R.id.tv_cancle)
        tvEnter = findViewById(R.id.tv_enter)
        tvIssue = findViewById(R.id.tv_issue)

        permView = findViewById(R.id.perm_view)
        ivUpdate = findViewById(R.id.iv_update)
        ivRemove = findViewById(R.id.iv_remove)

        issueLayout.visibility = GONE
        tvPrompt.visibility = GONE

        if (EaseRepository.instance.role == EaseConstant.ROLE_STUDENT || EaseRepository.instance.role == EaseConstant.ROLE_OBSERVE) {
            tvIssue.visibility = GONE
            permView.visibility = GONE
        } else {
            tvIssue.visibility = VISIBLE
            permView.visibility = VISIBLE
        }
    }

    override fun initListener() {
        super.initListener()
        tvIssue.setOnClickListener(this)
        noticeRoot.setOnClickListener(this)
        tvCancel.setOnClickListener(this)
        tvEnter.setOnClickListener(this)
        ivUpdate.setOnClickListener(this)
        ivRemove.setOnClickListener(this)
        etNotice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tvCount.text = String.format("%s%s", s.toString().length.toString(), context.getString(R.string.fcr_total_count))
                if (s.toString().length > 500) {
                    tvPrompt.visibility = VISIBLE
                    tvCount.setTextColor(ContextCompat.getColor(context, R.color.fcr_red))
                } else {
                    tvPrompt.visibility = GONE
                    tvCount.setTextColor(ContextCompat.getColor(context, R.color.fcr_gray))
                }
            }

            override fun afterTextChanged(s: Editable?) {

            }
        })

        etNotice.setOnFocusChangeListener { v, hasFocus ->
            if(hasFocus){
                etNotice.text?.length?.let { etNotice.setSelection(it) }
            }
        }


    }

    fun announcementChange(announcement: String){
        ThreadManager.instance.runOnMainThread {
            if (announcement.isNotEmpty()) {
                defaultLayout.visibility = GONE
                content.text = announcement
                content.visibility = VISIBLE
            } else {
                defaultLayout.visibility = VISIBLE
                content.visibility = GONE
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tv_issue -> showIssueLayout()
            R.id.notice_root -> CommonUtil.showSoftKeyboard(etNotice)
            R.id.tv_cancle -> {
                if(announcement.isNotEmpty()){
                    showAnnouncementView()
                }else{
                    showDefaultLayout()
                }
            }
            R.id.tv_enter -> {
                if(etNotice.text.toString().isNotEmpty()){
                    if (etNotice.text.toString().length > 500){
                        ThreadManager.instance.runOnMainThread{
                            Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_words_out_of_limit), Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        announcement = etNotice.text.toString()
                        showAnnouncementView()
                        updateAnnouncement(announcement)
                    }
                }else{
                    announcement = ""
                    showDefaultLayout()
                    updateAnnouncement(announcement)
                }
            }
            R.id.iv_update -> {
                announcement = content.text.toString()
                showIssueLayout()
            }
            R.id.iv_remove -> {
                announcement = ""
                showDefaultLayout()
                updateAnnouncement(announcement)
                viewEventListener?.onAnnouncementChange(announcement)
            }
        }
    }

    private fun showIssueLayout(){
        etNotice.setText(announcement)
        CommonUtil.showSoftKeyboard(etNotice)
        issueLayout.visibility = VISIBLE
        defaultLayout.visibility = GONE
    }

    private fun showDefaultLayout(){
        issueLayout.visibility = GONE
        defaultLayout.visibility = VISIBLE
    }

    private fun showAnnouncementView(){
        content.text = announcement
        issueLayout.visibility = GONE
        defaultLayout.visibility = GONE
    }

    private fun updateAnnouncement(announcement: String){
        ChatClient.getInstance().chatroomManager().asyncUpdateChatRoomAnnouncement(EaseRepository.instance.chatRoomId, announcement, object: CallBack {
            override fun onSuccess() {
                viewEventListener?.onAnnouncementChange(announcement)
                announcementChange(announcement)
            }

            override fun onError(code: Int, error: String?) {
                ThreadManager.instance.runOnMainThread{
                    Toast.makeText(context, context.getString(R.string.fcr_hyphenate_im_release_announcement_failed), Toast.LENGTH_SHORT).show()
                }
            }

            override fun onProgress(progress: Int, status: String?) {

            }
        })
    }

    override fun loadMessageFinish(messages: List<ChatMessage>) {
        // 不需要处理
    }

    override fun loadHistoryMessageFinish() {
        // 不需要处理
    }

    override fun fetchAnnouncementFinish(announcement: String) {
        announcementChange(announcement)
    }

    override fun fetchChatRoomMutedStatus(allMuted: Boolean) {
        // 不需要处理
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        easeRepository.addOperationListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        easeRepository.removeOperationListener(this)
    }

}