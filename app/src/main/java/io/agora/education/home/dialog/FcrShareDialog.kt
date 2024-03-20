package io.agora.education.home.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import io.agora.education.utils.AppUtil
import io.agora.education.R
import io.agora.education.databinding.DialogShareRoomLinkBinding


/**
 * author : felix
 * date : 2022/9/5
 * description :
 */
class FcrShareDialog(context: Context) : FcrBaseSheetDialog(context) {
    var linkUrl = ""
    lateinit var binding: DialogShareRoomLinkBinding

    fun setLinkUrl(linkUrl: String): FcrShareDialog {
        this.linkUrl = linkUrl
        return this
    }

    companion object {
        fun newInstance(context: Context): FcrShareDialog {
            return FcrShareDialog(context)
        }
    }

    override fun getView(): View {
        binding = DialogShareRoomLinkBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun initView() {
        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.tvCopyLink.setOnClickListener {
            AppUtil.copyToClipboard(context, linkUrl)
            Toast.makeText(context, context.getString(R.string.fcr_join_copy_success), Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}