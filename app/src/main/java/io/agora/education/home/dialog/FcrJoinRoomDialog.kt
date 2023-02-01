package io.agora.education.home.dialog

import android.Manifest
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.internal.base.PreferenceManager
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.launch.*
import io.agora.education.R
import io.agora.education.config.AppConstants
import io.agora.education.databinding.DialogJoinRoomBinding
import io.agora.education.utils.AppUtil


/**
 * author : hefeng
 * date : 2022/9/6
 * description : join room
 */
class FcrJoinRoomDialog(context: Context) : FcrBaseSheetDialog(context) {
    lateinit var binding: DialogJoinRoomBinding
    var roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.value
    val TAG = "JoinRoom"
    var fcrJoinRoom: FcrJoinRoom? = null

    companion object {
        fun newInstance(context: Context): FcrJoinRoomDialog {
            return FcrJoinRoomDialog(context)
        }
    }

    override fun getView(): View {
        binding = DialogJoinRoomBinding.inflate(LayoutInflater.from(context))
        return binding.root
    }

    override fun initView() {
        fcrJoinRoom = FcrJoinRoom(context)
        fcrJoinRoom?.setJoinRoomListener = { isJoinSuccess, isDismissDailg, message ->
            loading.dismiss()
            if (isJoinSuccess || isDismissDailg) {
                dismiss()
            }
        }

        val nickName = PreferenceManager.get(AppConstants.KEY_SP_NICKNAME, "")
        binding.etJoinUserName.setText(nickName)

        binding.ivClose.setOnClickListener {
            dismiss()
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.tvJoin.setOnClickListener {
            val roomId = binding.etJoinRoomId.text.toString()
            val userName = binding.etJoinUserName.text.toString()

            if (TextUtils.isEmpty(roomId)) {
                val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
                binding.layoutRoomId.startAnimation(shake)
                ToastManager.showShort(R.string.fcr_joinroom_tips_roomid_empty)
                return@setOnClickListener
            }


            if (TextUtils.isEmpty(userName)) {
                val shake: Animation = AnimationUtils.loadAnimation(context, R.anim.fcr_input_shake)
                binding.layoutUserName.startAnimation(shake)
                ToastManager.showShort(R.string.fcr_joinroom_tips_username_empty)
                return@setOnClickListener
            }

            if (AppUtil.isFastClick()) {
                return@setOnClickListener
            }

            val act = context as AppCompatActivity
            PermissionX.init(act)
                .permissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        loading.show()
                        fcrJoinRoom?.joinRoom(roomId, roleType, userName)
                    } else {
                        Toast.makeText(context, R.string.no_enough_permissions, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvTeacher.setOnClickListener {
            binding.ivTeacherCheck.visibility = View.VISIBLE
            binding.ivStudentCheck.visibility = View.GONE

            setSelectStyle(true, binding.tvTeacher)
            setSelectStyle(false, binding.tvStudent)

            roleType = AgoraEduRoleType.AgoraEduRoleTypeTeacher.value
        }
        binding.tvStudent.setOnClickListener {
            setSelectStyle(false, binding.tvTeacher)
            setSelectStyle(true, binding.tvStudent)

            binding.ivTeacherCheck.visibility = View.GONE
            binding.ivStudentCheck.visibility = View.VISIBLE

            roleType = AgoraEduRoleType.AgoraEduRoleTypeStudent.value
        }
    }

    fun setSelectStyle(isSelect: Boolean, view: TextView) {
        view.setBackgroundResource(if (isSelect) R.drawable.bg_join_rect_blue else R.drawable.bg_join_rect_white)
        view.setTextColor(if (isSelect) Color.WHITE else Color.BLACK)
    }
}