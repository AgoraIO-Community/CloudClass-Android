package io.agora.education.home.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import io.agora.education.databinding.FcrLinkInputDialogBinding
import io.agora.education.utils.FcrSoftKeyBoardListener
import java.util.*


/**
 * author : wufang
 * date : 2022/9/15
 * description : create room
 */
class FcrLinkInputDialog(context: Context) : FcrBaseDialog(context) {
    lateinit var binding: FcrLinkInputDialogBinding
    lateinit var softKeyBoardListener: FcrSoftKeyBoardListener
    var onInputListener: ((String) -> Unit)? = null

    constructor(context: Context, playbackUrl: String) : this(context) {
        val length = playbackUrl.length
        binding.fcrEtInputPlaybackLink.setText(playbackUrl)
        binding.fcrEtInputPlaybackLink.setSelection(length)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initView() {
        binding.fcrEtInputPlaybackLink.isFocusable = true
        binding.fcrEtInputPlaybackLink.isFocusableInTouchMode = true
        binding.fcrEtInputPlaybackLink.requestFocus()
        binding.fcrEtInputPlaybackLink.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                //TextView显示字数
                if (s.isEmpty()) {
                    binding.fcrInputCount.visibility = View.GONE
                } else {
                    binding.fcrInputCount.visibility = View.VISIBLE
                }
                binding.fcrInputCount.text = s.length.toString()
            }
        })

        binding.fcrInputClose.setOnClickListener {
            binding.fcrEtInputPlaybackLink.setText("")
        }

        //使软键盘能正常弹出
        Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    val inputManager =
                        binding.fcrEtInputPlaybackLink.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.showSoftInput(binding.fcrEtInputPlaybackLink, 0)
                }
            },
            200
        )

        binding.ivConfirm.setOnClickListener {
            val text = binding.fcrEtInputPlaybackLink.text.toString()
            if (TextUtils.isEmpty(text)) {
                return@setOnClickListener
            }
            onInputListener?.invoke(binding.fcrEtInputPlaybackLink.text.toString())
            dismiss()
        }

        binding.root.setOnClickListener { //点黑色背景，退出dialog
            dismiss()
        }
        setKeyBoardListener()
    }

    fun setKeyBoardListener() {
        //完美解决键盘顶起问题
        if (!this::softKeyBoardListener.isInitialized) {
            softKeyBoardListener = FcrSoftKeyBoardListener(binding.root)
        }
        softKeyBoardListener.setListener(object : FcrSoftKeyBoardListener.OnSoftKeyBoardChangeListener {
            override fun keyBoardShow(height: Int) {
                binding.root.translationY = -height * 1f
            }

            override fun keyBoardHide(height: Int) {
                dismiss()
            }
        })
    }

    fun setMarginBottom(distance: Int) {
        val params = binding.layoutInput.layoutParams as FrameLayout.LayoutParams
        params.setMargins(0, 0, 0, distance)
        binding.layoutInput.layoutParams = params
    }

    override fun getView(): View {
        binding = FcrLinkInputDialogBinding.inflate(LayoutInflater.from(context))
        // full screen
        val dm = Resources.getSystem().displayMetrics
        binding.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPixels)
        return binding.root
    }

    override fun dismiss() {
        super.dismiss()
        binding.root.translationY = 0f
        softKeyBoardListener.destroy()
    }
}