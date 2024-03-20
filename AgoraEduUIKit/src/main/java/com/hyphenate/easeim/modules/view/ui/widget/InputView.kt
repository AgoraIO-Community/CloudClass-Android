package com.hyphenate.easeim.modules.view.ui.widget

import android.Manifest
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.agoraeduuikit.R
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.InputMsgListener
import com.hyphenate.easeim.modules.view.adapter.EmojiGridAdapter
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.util.VersionUtils

/**
 * 输入框控件
 */
class InputView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr), View.OnClickListener {
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    lateinit var inputRoot: RelativeLayout
    lateinit var bottomView: LinearLayout
    lateinit var editContent: AppCompatEditText
    lateinit var textCount: TextView
    lateinit var faceView: RelativeLayout
    lateinit var normalFace: AppCompatImageView
    lateinit var checkedFace: AppCompatImageView
    lateinit var ivImage: AppCompatImageView
    lateinit var btnSend: AppCompatTextView
    lateinit var emojiView: GridView
    lateinit var emojiAdapter: EmojiGridAdapter
    private val emojiList = arrayOf("\uD83D\uDE0A", "\uD83D\uDE03", "\uD83D\uDE09", "\uD83D\uDE2E", "\uD83D\uDE0B", "\uD83D\uDE0E", "\uD83D\uDE21", "\uD83D\uDE16", "\uD83D\uDE33", "\uD83D\uDE1E", "\uD83D\uDE2D", "\uD83D\uDE10", "\uD83D\uDE07", "\uD83D\uDE2C", "\uD83D\uDE06", "\uD83D\uDE31", "\uD83C\uDF85", "\uD83D\uDE34", "\uD83D\uDE15", "\uD83D\uDE37", "\uD83D\uDE2F", "\uD83D\uDE0F", "\uD83D\uDE11", "\uD83D\uDC96", "\uD83D\uDC94", "\uD83C\uDF19", "\uD83C\uDF1F", "\uD83C\uDF1E", "\uD83C\uDF08", "\uD83D\uDE1A", "\uD83D\uDE0D", "\uD83D\uDC8B", "\uD83C\uDF39", "\uD83C\uDF42", "\uD83D\uDC4D")
    var inputMsgListener: InputMsgListener? = null
    var maxMessageLength = 3 // 单条消息最大长度

    companion object {
        private const val TAG = "InputView"
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.fcr_input_layout, this)
        initView()
    }

    private fun initView(){
        inputRoot = findViewById(R.id.input_root)
        bottomView = findViewById(R.id.bottom_root)
        editContent = findViewById(R.id.edit_content)
        textCount = findViewById(R.id.tv_input_count)
        faceView = findViewById(R.id.face_view)
        normalFace = findViewById(R.id.face_normal)
        checkedFace = findViewById(R.id.face_checked)
        ivImage = findViewById(R.id.iv_image)
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
        ivImage.setOnClickListener(this)
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
                textCount.text = "" + s.toString().length
                if (s.toString().length == 300) {
                    textCount.setTextColor(ContextCompat.getColor(context, R.color.fcr_text_red))
                } else {
                    textCount.setTextColor(ContextCompat.getColor(context, R.color.fcr_text_level3_color))
                }
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

    private fun clickImage() {
        CommonUtil.hideSoftKeyboard(editContent)
        if (VersionUtils.isTargetQ(context)) {
            inputMsgListener?.onSelectImage()
        } else {
            val act = context as AppCompatActivity
            PermissionX.init(act)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        inputMsgListener?.onSelectImage()
                    } else {
                        Toast.makeText(context, "No enough permissions", Toast.LENGTH_SHORT).show()
                    }
                }
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
            editContent.text?.clear()
            inputMsgListener?.onSendMsg(msgContent)
        }
        CommonUtil.hideSoftKeyboard(editContent)
    }

    private fun clickEmojiItem(emoji: String) {
        editContent.text?.append(emoji)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.edit_content -> {if(normalFace.visibility != VISIBLE) hideFaceView()}
            R.id.face_view -> clickFace()
            R.id.iv_image -> clickImage()
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