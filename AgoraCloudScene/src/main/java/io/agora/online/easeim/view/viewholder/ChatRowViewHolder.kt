package io.agora.online.easeim.view.viewholder

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import io.agora.online.easeim.constant.EaseConstant
import io.agora.online.easeim.view.`interface`.MessageListItemClickListener
import io.agora.CallBack
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.online.R
import io.agora.online.provider.AgoraIMUserInfo
import io.agora.chat.ChatClient
import io.agora.chat.ChatMessage
import io.agora.util.EMLog
import java.lang.Exception


abstract class ChatRowViewHolder(
    view: View,
    val itemClickListener: MessageListItemClickListener,
    val context: Context
) : RecyclerView.ViewHolder(view) {
    companion object {
        private const val TAG = "ChatRowViewHolder"
    }

    private val avatar: ImageView? = itemView.findViewById(R.id.iv_avatar)
    val name: TextView? = itemView.findViewById(R.id.tv_name)
    val privateView: TextView? = itemView.findViewById(R.id.tv_name_private)
    val privateView1: TextView? = itemView.findViewById(R.id.tv_name_private1)
    val privateView2: TextView? = itemView.findViewById(R.id.tv_name_private2)
    val role: TextView? = itemView.findViewById(R.id.tv_role)
    private val proBar: ProgressBar? = itemView.findViewById(R.id.progress_bar)
    private val reSend: ImageView? = itemView.findViewById(R.id.resend)
    private val recall: TextView? = itemView.findViewById(R.id.tv_recall)
    private val mute: TextView? = itemView.findViewById(R.id.tv_mute)

    private val title: LinearLayout? =
        itemView.findViewById(R.id.title)
    private val privateChatOpsBox: LinearLayout? =
        itemView.findViewById(R.id.ll_private_chat_ops_box)
    private val privateChatClose: ImageView? = itemView.findViewById(R.id.iv_private_chat_close)
    private val privateChatGo: TextView? = itemView.findViewById(R.id.tv_private_chat_go)

    lateinit var message: ChatMessage
    val mainThreadHandler = Handler(Looper.getMainLooper())
    private val callback = ChatCallback()

    open fun setUpView(message: ChatMessage, currentUser: String) {
        this.message = message
        avatar?.let {
            Glide.with(context).load(
                message.getStringAttribute(EaseConstant.AVATAR_URL, "")
            ).apply(RequestOptions.bitmapTransform(CircleCrop())).error(R.mipmap.fcr_default_avatar)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(
                    avatar
                )
        }

        val msgType =
            message.getIntAttribute(EaseConstant.MSG_TYPE, EaseConstant.NORMAL_MSG)
        val nickName = message.getStringAttribute(EaseConstant.NICK_NAME, "")

        var fullTipsForMessage = ""

        var receiverListInString: String? = null
        var receiverList: List<AgoraIMUserInfo>? = null

        try {
            receiverListInString = message.ext().get(EaseConstant.RECEIVER_LIST) as String
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val PRIVATE_MSG_TAG = "(Private)"

        try {
            receiverList = GsonUtil.gson.fromJson(
                receiverListInString,
                object : TypeToken<List<AgoraIMUserInfo>>() {}.type
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        privateView?.visibility = View.GONE
        privateView1?.visibility = View.GONE
        privateView2?.visibility = View.GONE

        receiverList?.forEach {
            if (it != null) {
                if (message.from.equals(currentUser)) {
                    //fullTipsForMessage += "Sent to ${it.nickName}"
                    fullTipsForMessage = it.nickName
                    privateView?.visibility = View.VISIBLE
                    privateView1?.visibility = View.VISIBLE
                    privateView2?.visibility = View.VISIBLE
                } else if (it.userId == currentUser) {
                    //fullTipsForMessage += "Sent from $nickName"
                    fullTipsForMessage = nickName
                    privateView?.visibility = View.VISIBLE
                    privateView1?.visibility = View.VISIBLE
                    privateView2?.visibility = View.VISIBLE
                }
//                fullTipsForMessage += PRIVATE_MSG_TAG
//                if (fullTipsForMessage.length > PRIVATE_MSG_TAG.length) {
//                    return@forEach
//                }
            }
        }

        if (fullTipsForMessage.isEmpty()) {
            fullTipsForMessage = nickName
        }

//        val spannableString = SpannableString(fullTipsForMessage)
//        val highlightIndex = fullTipsForMessage.indexOf(PRIVATE_MSG_TAG)
//        if (highlightIndex > 0) {
//            val foregroundColorSpan =
//                ForegroundColorSpan(context.getColor(R.color.fcr_v2_yellow))
//            spannableString.setSpan(
//                foregroundColorSpan,
//                highlightIndex,
//                highlightIndex + 9,
//                Spanned.SPAN_INCLUSIVE_EXCLUSIVE
//            )
//            //name?.textSize = 8f
//        }
        name?.text = fullTipsForMessage

        if (message.getIntAttribute(
                EaseConstant.ROLE,
                EaseConstant.ROLE_STUDENT
            ) == EaseConstant.ROLE_TEACHER
        ) {
            role?.text = context.getString(R.string.fcr_hyphenate_im_teacher)
            role?.visibility = View.VISIBLE
        } else if (message.getIntAttribute(
                EaseConstant.ROLE,
                EaseConstant.ROLE_STUDENT
            ) == EaseConstant.ROLE_ASSISTANT
        ) {
            role?.text = context.getString(R.string.fcr_hyphenate_im_assistant)
            role?.visibility = View.VISIBLE
        } else {
            role?.visibility = View.GONE
        }

        onSetUpView()
        setListener()
        handleMessage()
    }

    abstract fun onSetUpView()

    open fun setListener() {
        // Reset everytime when view freshed
        privateChatOpsBox?.visibility = View.INVISIBLE
        title?.visibility = View.VISIBLE

        reSend?.setOnClickListener {
            itemClickListener.onResendClick(message)
        }

        avatar?.setOnClickListener {
            // Check if current user
            if (ChatClient.getInstance().currentUser.equals(message.from)) {
                // Return for sending to target user
                return@setOnClickListener
            }

            title?.visibility = View.INVISIBLE
            privateChatOpsBox?.findViewById<ImageView>(R.id.iv_avatar2)
                ?.setImageDrawable(avatar?.drawable)
            privateChatOpsBox?.bringToFront()
            privateChatOpsBox?.visibility = View.VISIBLE
        }

        privateChatClose?.setOnClickListener {
            privateChatOpsBox?.visibility = View.INVISIBLE
            title?.visibility = View.VISIBLE
        }

        privateChatGo?.setOnClickListener {
            privateChatClose?.performClick()
            itemClickListener.onPrivateChatViewDisplayed(message)
        }
    }

    private fun handleMessage() {
        message.setMessageStatusCallback(callback)
        mainThreadHandler.post {
            when (message.status()) {
                ChatMessage.Status.CREATE -> onMessageCreate()
                ChatMessage.Status.SUCCESS -> onMessageSuccess()
                ChatMessage.Status.INPROGRESS -> onMessageInProgress()
                ChatMessage.Status.FAIL -> onMessageError()
                else -> EMLog.e(TAG, "default status")
            }
        }
    }

    inner class ChatCallback : CallBack {
        override fun onSuccess() {
            mainThreadHandler.post {
                onMessageSuccess()
            }
        }

        override fun onError(code: Int, error: String?) {
            mainThreadHandler.post {
                onMessageError()
                itemClickListener.onMessageError(message, code, error)
            }
        }

        override fun onProgress(progress: Int, status: String?) {
            mainThreadHandler.post {
                onMessageInProgress()
            }
        }

    }

    private fun onMessageCreate() {
        setStatus(View.VISIBLE, View.GONE)
    }

    open fun onMessageSuccess() {
        setStatus(View.GONE, View.GONE)
    }

    fun onMessageError() {
        setStatus(View.GONE, View.VISIBLE)
    }

    open fun onMessageInProgress() {
        setStatus(View.VISIBLE, View.GONE)
    }

    private fun setStatus(progressVisible: Int, reSendVisible: Int) {
        proBar?.visibility = progressVisible
        reSend?.visibility = reSendVisible
    }
}
