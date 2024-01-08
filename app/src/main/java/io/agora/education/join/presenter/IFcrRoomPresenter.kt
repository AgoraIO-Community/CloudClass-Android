package io.agora.education.join.presenter

import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import io.agora.education.home.dialog.FcrJoinRoom

/**
 * author : felix
 * date : 2023/8/8
 * description :
 */
abstract class IFcrRoomPresenter(var context: AppCompatActivity) {
    var isAgreePrivateTerms = false
    var onPrivateTermsCheckUpdate: ((Boolean) -> Unit)? = null
    var loading: AgoraLoadingDialog = AgoraLoadingDialog(context)
    var fcrJoinRoom = FcrJoinRoom(context)

    abstract fun updatePrivateTermsView(isAgree: Boolean)

    abstract fun shakePrivateTermsView()

    abstract fun hiddenTips()

    abstract fun joinRoom()

}