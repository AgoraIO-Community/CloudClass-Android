package com.agora.edu.component.teachaids

import android.app.Dialog
import android.content.Context
import android.view.*
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraRightToLeftLayoutBinding


class RightToLeftDialog(var context: Context){
    var dialog: Dialog
    var parentView: View
    lateinit var binding: AgoraRightToLeftLayoutBinding

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
        dialog.dismiss()
    }

    fun getView(): View {
        binding = AgoraRightToLeftLayoutBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    fun initView() {
        binding.mycloudFileHelpClose.setOnClickListener {
            dismiss()
        }

        binding.rightToLeftLayout.setOnClickListener {
            dismiss()
        }
    }
    fun isCanTouchClose(): Boolean {
        return true
    }

}