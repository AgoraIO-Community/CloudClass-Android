package io.agora.education.home.dialog

import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.education.R


/**
 * author : felix
 * date : 2022/9/5
 * description : 无下拉的行为
 */
abstract class FcrBaseDialog(var context: Context) {
    var bottomSheetDialog: Dialog
    var parentView: View
    var loading: AgoraLoadingDialog = AgoraLoadingDialog(context)
    var heightPixels: Int = 0
    var widthPixels: Int = 0

    init {
        parentView = this.getView()
        bottomSheetDialog = Dialog(context,R.style.single_fullscreen_dialog_theme)
        //bottomSheetDialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        bottomSheetDialog.setContentView(parentView)
        bottomSheetDialog.setCancelable(isCanTouchClose())
        bottomSheetDialog.setCanceledOnTouchOutside(isCanTouchClose())
        this.initView()
    }

    open fun show() {
        bottomSheetDialog.show()

        val window: Window? = bottomSheetDialog.window
        window?.setBackgroundDrawable(BitmapDrawable())
        val lp: WindowManager.LayoutParams? = window?.attributes
        lp?.windowAnimations = R.style.BottomInAndOutStyle
        lp?.height = heightPixels //设置宽度
        lp?.width = widthPixels //设置宽度
        window?.attributes = lp
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