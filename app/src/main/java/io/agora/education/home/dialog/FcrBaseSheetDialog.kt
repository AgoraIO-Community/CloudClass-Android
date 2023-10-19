package io.agora.education.home.dialog

import android.content.Context
import android.view.View
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.education.R


/**
 * author : felix
 * date : 2022/9/5
 * description :
 */
abstract class FcrBaseSheetDialog(var context: Context) {
    protected var bottomSheetDialog: BottomSheetDialog
    var parentView: View
    var loading: AgoraLoadingDialog = AgoraLoadingDialog(context)

    init {
        parentView = this.getView()
        bottomSheetDialog = BottomSheetDialog(context)
        // full
        bottomSheetDialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.behavior.skipCollapsed = true
        bottomSheetDialog.behavior.isHideable = isCanTouchClose()

        bottomSheetDialog.setContentView(parentView)
        bottomSheetDialog.setCancelable(isCanTouchClose())
        bottomSheetDialog.setCanceledOnTouchOutside(isCanTouchClose())
        this.initView()
    }

    open fun show() {
        bottomSheetDialog.show()
        bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    open fun dismiss() {
        loading.dismiss()
        bottomSheetDialog.dismiss()
    }

    abstract fun getView(): View

    abstract fun initView()

    open fun isCanTouchClose(): Boolean {
        return true
    }
}