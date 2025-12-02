package io.agora.online.dialog

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import io.agora.online.loading.AgoraLoadingDialog
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineShareDialogBinding


/**
 * author : felix
 * date : 2022/9/23
 * description :
 */
class FcrShareDialog(var context: Context) {
    var dialog: Dialog
    var parentView: View
    var loading: AgoraLoadingDialog = AgoraLoadingDialog(context)
    lateinit var binding: FcrOnlineShareDialogBinding

    init {
        parentView = this.getView()
        dialog = Dialog(context)
        dialog.setContentView(parentView)
        dialog.setCancelable(isCanTouchClose())
        dialog.setCanceledOnTouchOutside(isCanTouchClose())
        this.initView()
    }

    fun show() {
        dialog.show()
        val window: Window? = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        val lp: WindowManager.LayoutParams? = window?.attributes
        lp?.windowAnimations = R.style.RightInAndOutStyle
        lp?.height = ViewGroup.LayoutParams.MATCH_PARENT
        lp?.width = ViewGroup.LayoutParams.MATCH_PARENT
        window?.attributes = lp
    }

    fun dismiss() {
        loading.dismiss()
        dialog.dismiss()
    }

    fun getView(): View {
        binding = FcrOnlineShareDialogBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    fun initView() {
        binding.fcrRoomId.setOnClickListener {
            // copy id
            val roomId = binding.fcrRoomId.text.toString()
            copyToClipboard(context, roomId)
            Toast.makeText(context, context.getString(R.string.fcr_join_copy_success), Toast.LENGTH_SHORT).show()
        }

        binding.fcrRoomShare.setOnClickListener {
            // 分享链接
            val linkUrl = binding.fcrRoomLink.text.toString()
            shareUrl(context, linkUrl)
        }

        binding.fcrRoomCopyLink.setOnClickListener {
            val linkUrl = binding.fcrRoomLink.text.toString()
            copyToClipboard(context, linkUrl)
            Toast.makeText(context, context.getString(R.string.fcr_join_copy_success), Toast.LENGTH_SHORT).show()
        }

        binding.fcrRoomShareClose.setOnClickListener {
            dismiss()
        }

        binding.fcrShareContainer.setOnClickListener {
            dismiss()
        }
    }

    fun setShareLink(linkUrl: String) {
        binding.fcrRoomLink.text = linkUrl
    }

    fun setRoomId(roomId: String) {
        binding.fcrRoomId.text = roomId
    }

    fun isCanTouchClose(): Boolean {
        return true
    }

    fun copyToClipboard(context: Context, text: String?) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val mClipData = ClipData.newPlainText("agora", text)
        cm.setPrimaryClip(mClipData)
    }


    /**
     * 分享文本
     *
     * @param context
     * @param path
     */
    fun shareUrl(context: Context, content: String?) {
        if (TextUtils.isEmpty(content)) {
            return
        }

        val it = Intent(Intent.ACTION_SEND)
        it.putExtra(Intent.EXTRA_TEXT, content)
        it.type = "text/plain"
        context.startActivity(Intent.createChooser(it, "Share"))
    }
}