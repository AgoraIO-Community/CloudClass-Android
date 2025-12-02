package io.agora.education.home

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import io.agora.education.databinding.DialogPrivacyTermsBottomBinding
import io.agora.education.home.dialog.FcrBaseSheetDialog
import io.agora.education.utils.FcrPrivateProtocolUtils

/**
 * author : felix
 * date : 2023/7/28
 * description :
 */

class FcrPrivacyTermsBottomDialog(context: Context) : FcrBaseSheetDialog(context) {
    private lateinit var binding: DialogPrivacyTermsBottomBinding
    var onAgreeListener: ((Boolean) -> Unit)? = null

    companion object {
        fun newInstance(context: Context): FcrPrivacyTermsBottomDialog = FcrPrivacyTermsBottomDialog(context)
    }

    override fun getView(): View {
        binding = DialogPrivacyTermsBottomBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun initView() {
        binding.fcrTvPrivateTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.fcrTvPrivateTerms.text = FcrPrivateProtocolUtils.getPrivateProtocolInfo(context) {

        }

        binding.btnAccept.setOnClickListener {
            dismiss()
            onAgreeListener?.invoke(true)
        }

        binding.btnDecline.setOnClickListener {
            dismiss()
            onAgreeListener?.invoke(false)
        }
    }

    override fun isCanTouchClose(): Boolean {
        return false
    }
}