package com.hyphenate.easeim.modules.view.ui.widget

import android.app.ProgressDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import io.agora.agoraeduuikit.R
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.ui.widget.photoview.EasePhotoView
import io.agora.CallBack
import io.agora.chat.ChatClient
import io.agora.chat.ChatMessage
import io.agora.chat.ImageMessageBody
import io.agora.util.EMLog

class ShowImageView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : RelativeLayout(context, attributeSet, defStyleAttr){
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    lateinit var progressDialog: ProgressDialog
    lateinit var imageView: EasePhotoView
    lateinit var message: ChatMessage
    var chatPagerListener: ChatPagerListener? = null

    //伴生对象
    companion object {
        private const val TAG = "ShowImageView"
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.fcr_show_image_layout, this)
        initView()
    }

    private fun initView(){
        imageView = findViewById(R.id.imageView)
        imageView.setOnViewTapListener { view, x, y ->
            chatPagerListener?.onCloseImage()
        }
        progressDialog = ProgressDialog(context)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun setUpView(){
        Glide.with(context).load("").into(imageView)
        val body = message.body as ImageMessageBody
        val imgUri = body.localUri
        CommonUtil.takePersistableUriPermission(context, imgUri)
        if(CommonUtil.isFileExistByUri(context, imgUri)){
            Glide.with(context).load(imgUri).into(imageView)
        }else{
            downloadImage()
        }
    }

    private fun downloadImage(){
        val msg = context.getString(R.string.fcr_hyphenate_im_download_picture)
        progressDialog.setMessage(msg)
        progressDialog.show()
        message.setMessageStatusCallback(object: CallBack {
            override fun onSuccess() {
                EMLog.e(TAG, "onSuccess")
                progressDialog.dismiss()
                ThreadManager.instance.runOnMainThread{
                    val body = message.body as ImageMessageBody
                    val imgUri = body.localUri
                    Glide.with(context).load(imgUri).into(imageView)
                }
            }

            override fun onError(code: Int, error: String?) {
                EMLog.e(TAG, "onError")
                ThreadManager.instance.runOnMainThread{
                    imageView.setImageResource(R.mipmap.fcr_default_img)
                    progressDialog.dismiss()
                }
            }

            override fun onProgress(progress: Int, status: String?) {
                EMLog.e(TAG, "progress:$progress")
                ThreadManager.instance.runOnMainThread{
                    progressDialog.setMessage("$msg  $progress%")
                }
            }
        })

        ChatClient.getInstance().chatManager().downloadAttachment(message)
    }

    fun loadImage(message: ChatMessage){
        this.message = message
        setUpView()
    }

}