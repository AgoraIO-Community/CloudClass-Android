package io.agora.education.join.presenter

import android.Manifest
import android.app.Activity
import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.proxy.RoomType
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.education.R
import io.agora.education.databinding.LayoutCreateRoomBinding
import io.agora.education.home.dialog.FcrJoinRoom
import io.agora.education.join.FcrSelectRoomTypeDialog
import io.agora.education.join.FcrTextWatcher
import io.agora.education.utils.AppUtil
import io.agora.education.utils.FcrPrivateProtocolUtils

/**
 * author : felix
 * date : 2023/8/3
 * description : join room
 */
class FcrCreateRoomPresenter(context: AppCompatActivity, var binding: LayoutCreateRoomBinding) :
    IFcrRoomPresenter(context) {
    var roomType = FcrFcrRoomTypeHelper.getDefRoomType(context)

    fun initView() {
        initInputView()
        initPrivacyPolicy()
    }

    fun initPrivacyPolicy() {
        binding.fcrCreateImgCheck.isSelected = isAgreePrivateTerms

        binding.fcrTvCreatePrivate.movementMethod = LinkMovementMethod.getInstance()
        binding.fcrTvCreatePrivate.text = FcrPrivateProtocolUtils.getPrivateProtocol(context) {
            binding.fcrLayoutCreatePrivate.performClick()
        }
        binding.fcrLayoutCreatePrivate.setOnClickListener {
            isAgreePrivateTerms = !binding.fcrCreateImgCheck.isSelected
            updatePrivateTermsView(isAgreePrivateTerms)
            onPrivateTermsCheckUpdate?.invoke(isAgreePrivateTerms)
        }
    }

    fun initInputView() {
        fcrJoinRoom.setJoinRoomListener = { isJoinSuccess, isDismissDialog, message ->
            loading.dismiss()

            if (isJoinSuccess) {
                context.finish()
            }
        }

        binding.fcrInputRoomName.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val roomId = s.toString()
                if (roomId.isNotEmpty()) {
                    binding.fcrRoomNameClear.visibility = View.VISIBLE
                } else {
                    binding.fcrRoomNameClear.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrRoomNameClear.setOnClickListener {
            binding.fcrRoomNameClear.visibility = View.INVISIBLE
            binding.fcrInputRoomName.setText("")
        }

        binding.fcrInputNickName.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val roomId = s.toString()
                if (roomId.isNotEmpty()) {
                    binding.fcrNickClear.visibility = View.VISIBLE
                } else {
                    binding.fcrNickClear.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrNickClear.setOnClickListener {
            binding.fcrNickClear.visibility = View.INVISIBLE
            binding.fcrInputNickName.setText("")
        }

        binding.fcrClassTypeView.setOnClickListener {
            val roomTypeDialog = FcrSelectRoomTypeDialog.newInstance(context, roomType)
            roomTypeDialog.onSelectRoomType = {
                roomType = it
                binding.fcrRoomType.text = it.name
                roomTypeDialog.dismiss()
            }
            roomTypeDialog.show()
        }

        binding.fcrCreateBtn.setOnClickListener {
            joinRoom()
        }
    }

    override fun updatePrivateTermsView(isAgree: Boolean) {
        isAgreePrivateTerms = isAgree
        binding.fcrCreateImgCheck.isSelected = isAgreePrivateTerms
        binding.fcrLayoutCreateTips.visibility = View.GONE
    }

    override fun shakePrivateTermsView() {
        val shake: Animation = AnimationUtils.loadAnimation(context.applicationContext, R.anim.fcr_input_shake)
        binding.fcrLayoutCreatePrivate.startAnimation(shake)
        binding.fcrLayoutCreateTips.visibility = View.VISIBLE
    }

    override fun hiddenTips() {
        binding.fcrLayoutCreateTips.visibility = View.GONE
    }

    override fun joinRoom() {
        val roomName = binding.fcrInputRoomName.text.toString()
        if (roomName.isEmpty()) {
            binding.fcrLayoutInputRoomName.startAnimation(AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake))
            ToastManager.showShort(context,R.string.fcr_create_label_roomname_empty)
            return
        }

        val userName = binding.fcrInputNickName.text.toString()
        if (userName.length < 2) { // 2-20
            val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
            binding.fcrLayoutInputNickName.startAnimation(shake)
            ToastManager.showShort(context,R.string.fcr_login_free_tips_content_length)
            return
        }

        if (!isAgreePrivateTerms) { // show tips
            shakePrivateTermsView()
            return
        }

        if (AppUtil.isFastClick()) {
            return
        }

        val roleType = AgoraEduRoleType.AgoraEduRoleTypeTeacher.value

        PermissionX.init(context)
            .permissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    loading.show()
                    fcrJoinRoom.createJoinRoom(userName, roomName, roleType, roomType.type.value)
                } else {
                    Toast.makeText(context, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show()
                }
            }
    }
}