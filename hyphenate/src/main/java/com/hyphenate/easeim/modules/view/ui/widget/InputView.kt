package com.hyphenate.easeim.modules.view.ui.widget

import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.view.isVisible
import com.hyphenate.EMCallBack
import com.hyphenate.EMError
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.InputMsgListener
import com.hyphenate.easeim.modules.view.adapter.EmojiGridAdapter
import com.hyphenate.util.EMLog
import org.json.JSONObject

/**
 * 输入框控件
 */
class InputView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), View.OnClickListener {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    lateinit var inputRoot: RelativeLayout
    lateinit var bottomView: LinearLayout
    lateinit var editContent: EditText
    lateinit var faceView: RelativeLayout
    lateinit var normalFace: ImageView
    lateinit var checkedFace: ImageView
    lateinit var btnSend: TextView
    lateinit var emojiView: GridView
    lateinit var emojiAdapter: EmojiGridAdapter
    private val emojiList = arrayOf("\uD83D\uDE0A", "\uD83D\uDE03", "\uD83D\uDE09", "\uD83D\uDE2E", "\uD83D\uDE0B", "\uD83D\uDE0E", "\uD83D\uDE21", "\uD83D\uDE16", "\uD83D\uDE33", "\uD83D\uDE1E", "\uD83D\uDE2D", "\uD83D\uDE10", "\uD83D\uDE07", "\uD83D\uDE2C", "\uD83D\uDE06", "\uD83D\uDE31", "\uD83C\uDF85", "\uD83D\uDE34", "\uD83D\uDE15", "\uD83D\uDE37", "\uD83D\uDE2F", "\uD83D\uDE0F", "\uD83D\uDE11", "\uD83D\uDC96", "\uD83D\uDC94", "\uD83C\uDF19", "\uD83C\uDF1F", "\uD83C\uDF1E", "\uD83C\uDF08", "\uD83D\uDE1A", "\uD83D\uDE0D", "\uD83D\uDC8B", "\uD83C\uDF39", "\uD83C\uDF42", "\uD83D\uDC4D")
    var inputMsgListener: InputMsgListener? = null
    var chatRoomId = ""
    var roomUuid = ""
    var nickName = ""
    var avatarUrl = ""

    companion object{
        private const val TAG = "InputView"
    }

    init{
        LayoutInflater.from(context).inflate(R.layout.input_layout, this)
        initView()
    }

    private fun initView(){
        inputRoot = findViewById(R.id.input_root)
        bottomView = findViewById(R.id.bottom_root)
        editContent = findViewById(R.id.edit_content)
        faceView = findViewById(R.id.face_view)
        normalFace = findViewById(R.id.face_normal)
        checkedFace = findViewById(R.id.face_checked)
        btnSend = findViewById(R.id.btn_send)
        emojiView = findViewById(R.id.emoji_grid)
        emojiView.numColumns = 8
        emojiAdapter = EmojiGridAdapter(context, 1, emojiList)
        emojiView.adapter = emojiAdapter
        initListener()
    }

    private fun initListener(){
        emojiView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            clickEmojiItem(emojiList[position])
        }
        inputRoot.setOnClickListener(this)
        editContent.setOnClickListener(this)
        faceView.setOnClickListener(this)
        btnSend.setOnClickListener(this)
        editContent.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event != null && KeyEvent.KEYCODE_ENTER ==
                    event.keyCode && KeyEvent.ACTION_DOWN == event.action
            ) {
                clickSend()
                true
            } else
                false
        }
        editContent.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { inputMsgListener?.onContentChange(it) }
            }
        })
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            // When visible, let the edit text have a focus,
            // thus it can immediately receive click events.
            editContent.requestFocus()
        } else {
            editContent.clearFocus()
        }
    }

    private fun clickFace() {
        if(normalFace.visibility != VISIBLE) {
            hideFaceView()
        }else {
            showFaceView()
        }
    }

    private fun clickSend() {
        val msgContent = editContent.text.toString()
        if (msgContent.isNotEmpty()) {
            val message = EMMessage.createTxtSendMessage(msgContent, chatRoomId)
            setExtBeforeSend(message)
            sendMessage(message)
            editContent.text.clear()
        }
        inputMsgListener?.onSendMsg()
        CommonUtil.hideSoftKeyboard(editContent)
    }

    private fun clickEmojiItem(emoji: String) {
        editContent.text.append(emoji)
    }

    private fun setExtBeforeSend(message: EMMessage) {
        message.setAttribute(EaseConstant.ROLE, EaseConstant.ROLE_STUDENT)
        message.setAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        message.setAttribute(EaseConstant.ROOM_UUID, roomUuid)
        message.setAttribute(EaseConstant.NICK_NAME, nickName)
        message.setAttribute(EaseConstant.AVATAR_URL, avatarUrl)
    }

    /***
     * 发送消息
     */
    private fun sendMessage(message: EMMessage) {
        message.chatType = EMMessage.ChatType.ChatRoom
        message.setMessageStatusCallback(object: EMCallBack{
            override fun onSuccess() {

            }

            override fun onError(code: Int, error: String?) {
                if (code == EMError.MESSAGE_INCLUDE_ILLEGAL_CONTENT) {
                    ThreadManager.instance.runOnMainThread {
                        Toast.makeText(context, context.getString(R.string.message_incloud_illegal_content), Toast.LENGTH_SHORT).show()
                    }
                }
                EMLog.e(TAG, "onMessageError:$code = $error")
            }

            override fun onProgress(progress: Int, status: String?) {

            }
        })
        EMClient.getInstance().chatManager().sendMessage(message)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.edit_content -> {if(normalFace.visibility != VISIBLE) hideFaceView()}
            R.id.face_view -> clickFace()
            R.id.btn_send -> clickSend()
            R.id.input_root -> {
                CommonUtil.hideSoftKeyboard(editContent)
                inputMsgListener?.onOutsideClick()
            }
        }
    }

    fun hideFaceView(){
        normalFace.visibility = VISIBLE
        checkedFace.visibility = GONE
        emojiView.visibility = GONE
        CommonUtil.showSoftKeyboard(editContent)
    }

    fun showFaceView(){
        normalFace.visibility = GONE
        checkedFace.visibility = VISIBLE
        editContent.requestFocus()
        CommonUtil.hideSoftKeyboard(editContent)
        handler.postDelayed({
            emojiView.visibility = VISIBLE
        }, 100)
    }

    fun isNormalFace(): Boolean{
        return normalFace.visibility == VISIBLE
    }
}