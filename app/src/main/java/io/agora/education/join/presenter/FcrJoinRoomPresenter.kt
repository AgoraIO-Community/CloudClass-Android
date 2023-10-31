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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.agora.edu.component.loading.AgoraLoadingDialog
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.launch.AgoraEduRoleType
import io.agora.education.R
import io.agora.education.databinding.LayoutJoinRoomBinding
import io.agora.education.home.dialog.FcrJoinRoom
import io.agora.education.join.FcrTextWatcher
import io.agora.education.utils.AppUtil
import io.agora.education.utils.FcrPrivateProtocolUtils

/**
 * author : felix
 * date : 2023/8/3
 * description : join room
 */
class FcrJoinRoomPresenter(context: AppCompatActivity, var binding: LayoutJoinRoomBinding) : IFcrRoomPresenter(context) {
    var roleType: Int = AgoraEduRoleType.AgoraEduRoleTypeStudent.value

    fun initView() {
        initRole()
        initInputView()
        initPrivacyPolicy()
    }

    fun initPrivacyPolicy() {
        binding.fcrJoinPrivateCheck.isSelected = isAgreePrivateTerms

        binding.fcrTvJoinPrivate.movementMethod = LinkMovementMethod.getInstance()
        binding.fcrTvJoinPrivate.text = FcrPrivateProtocolUtils.getPrivateProtocol(context) {
            binding.fcrLayoutJoinPrivate.performClick()
        }
        binding.fcrLayoutJoinPrivate.setOnClickListener {
            isAgreePrivateTerms = !binding.fcrJoinPrivateCheck.isSelected
            updatePrivateTermsView(isAgreePrivateTerms)
            onPrivateTermsCheckUpdate?.invoke(isAgreePrivateTerms)
        }
    }

    override fun updatePrivateTermsView(isAgree: Boolean) {
        isAgreePrivateTerms = isAgree
        binding.fcrJoinPrivateCheck.isSelected = isAgreePrivateTerms
        binding.fcrLayoutJoinTips.visibility = View.GONE
    }

    override fun hiddenTips() {
        binding.fcrLayoutJoinTips.visibility = View.GONE
    }

    override fun shakePrivateTermsView() {
        val shake: Animation = AnimationUtils.loadAnimation(context.applicationContext, R.anim.fcr_input_shake)
        binding.fcrLayoutJoinPrivate.startAnimation(shake)
        binding.fcrLayoutJoinTips.visibility = View.VISIBLE
    }

    fun initInputView() {
        fcrJoinRoom.setJoinRoomListener = { isJoinSuccess, isDismissDialog, message ->
            if(!context.isFinishing || !context.isDestroyed) {
                loading.dismiss()
            }

//            if (isJoinSuccess) {
//                context.finish()
//            }
        }

        binding.fcrInputRoomIdEdit.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val roomId = s.toString()
                if (roomId.isNotEmpty()) {
                    binding.fcrRoomIdClear.visibility = View.VISIBLE
                } else {
                    binding.fcrRoomIdClear.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrInputRoomIdEdit.addTextChangedListener(SpaceTextWatcher(binding.fcrInputRoomIdEdit, 3))

        binding.fcrNickNameEdit.addTextChangedListener(object : FcrTextWatcher() {
            override fun afterTextChanged(s: Editable?) {
                val nickName = s.toString()
                if (nickName.isNotEmpty()) {
                    binding.fcrNickNameClear1.visibility = View.VISIBLE
                } else {
                    binding.fcrNickNameClear1.visibility = View.INVISIBLE
                }
            }
        })

        binding.fcrRoomIdClear.setOnClickListener {
            binding.fcrRoomIdClear.visibility = View.INVISIBLE
            binding.fcrInputRoomIdEdit.setText("")
        }

        binding.fcrNickNameClear1.setOnClickListener {
            binding.fcrNickNameClear1.visibility = View.INVISIBLE
            binding.fcrNickNameEdit.setText("")
        }

        binding.fcrJoinBtn.setOnClickListener {
            joinRoom()
        }
    }

    override fun joinRoom() {
        var roomId = binding.fcrInputRoomIdEdit.text.toString()
        val userName = binding.fcrNickNameEdit.text.toString()

        roomId = roomId.replace(" ", "")

        if (roomId.length < 9) {
            val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
            binding.fcrLayoutInputRoomId.startAnimation(shake)
            ToastManager.showShort(context,R.string.fcr_login_free_tips_num_length)
            return
        }

        if (userName.length < 2) { // 2-20
            val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
            binding.fcrLayoutInputNickName.startAnimation(shake)
            ToastManager.showShort(context, R.string.fcr_login_free_tips_content_length)
            return
        }

        if (!isAgreePrivateTerms) { // show tips
            shakePrivateTermsView()
            return
        }

        if (AppUtil.isFastClick()) {
            return
        }

        PermissionX.init(context)
            .permissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    if(!context.isFinishing || !context.isDestroyed) {
                        loading.show()
                    }
                    fcrJoinRoom.joinQueryRoom(roomId, roleType, userName)
                } else {
                    Toast.makeText(context, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun initRole() {
        binding.fcrTvStudent.setOnClickListener {
            resetRoleItem()
            setRoleItem(binding.fcrTvStudent, binding.fcrStudentCheck)
            roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.value
        }

        binding.fcrTvTeacher.setOnClickListener {
            resetRoleItem()
            setRoleItem(binding.fcrTvTeacher, binding.fcrTeacherCheck)
            roleType = AgoraEduRoleType.AgoraEduRoleTypeTeacher.value
        }

        binding.fcrTvAudience.setOnClickListener {
            resetRoleItem()
            setRoleItem(binding.fcrTvAudience, binding.fcrAudienceCheck)
            roleType = AgoraEduRoleType.AgoraEduRoleTypeObserver.value
        }
    }

    fun resetRoleItem() {
        binding.fcrTvStudent.background =
            ContextCompat.getDrawable(binding.root.context, R.drawable.bg_join_rect_white2)
        binding.fcrTvTeacher.background =
            ContextCompat.getDrawable(binding.root.context, R.drawable.bg_join_rect_white2)
        binding.fcrTvAudience.background =
            ContextCompat.getDrawable(binding.root.context, R.drawable.bg_join_rect_white2)

        binding.fcrTvStudent.setTextColor(ContextCompat.getColor(binding.root.context, R.color.fcr_black))
        binding.fcrTvTeacher.setTextColor(ContextCompat.getColor(binding.root.context, R.color.fcr_black))
        binding.fcrTvAudience.setTextColor(ContextCompat.getColor(binding.root.context, R.color.fcr_black))

        binding.fcrStudentCheck.visibility = View.GONE
        binding.fcrTeacherCheck.visibility = View.GONE
        binding.fcrAudienceCheck.visibility = View.GONE
    }

    fun setRoleItem(itemView: TextView, checkView: View) {
        itemView.background = ContextCompat.getDrawable(binding.root.context, R.drawable.bg_join_rect_blue)
        itemView.setTextColor(ContextCompat.getColor(binding.root.context, R.color.fcr_white))
        checkView.visibility = View.VISIBLE
    }
}