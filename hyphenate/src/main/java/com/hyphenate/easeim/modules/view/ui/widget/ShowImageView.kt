package com.hyphenate.easeim.modules.view.ui.widget

import android.app.ProgressDialog
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.hyphenate.EMCallBack
import com.hyphenate.chat.EMClient
import com.hyphenate.chat.EMImageMessageBody
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.manager.ThreadManager
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.hyphenate.easeim.modules.view.`interface`.ChatPagerListener
import com.hyphenate.easeim.modules.view.ui.widget.photoview.EasePhotoView
import com.hyphenate.util.EMLog

class ShowImageView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : RelativeLayout(context, attributeSet, defStyleAttr){
    constructor(context: Context) : this(context, null, 0)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0)

    lateinit var progressDialog: ProgressDialog
    lateinit var imageView: EasePhotoView
    lateinit var message: EMMessage
    lateinit var back: AppCompatImageView
    var chatPagerListener: ChatPagerListener? = null

    //伴生对象
    companion object {
        private const val TAG = "ShowImageView"
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.show_image_layout, this)
        initView()
    }

    private fun initView(){
        imageView = findViewById(R.id.imageView)
        back = findViewById(R.id.close)
        back.setOnClickListener{
            chatPagerListener?.onCloseImage()
        }
        progressDialog = ProgressDialog(context)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCanceledOnTouchOutside(false)
    }

    private fun setUpView(){
        Glide.with(context).load("").into(imageView)
        val body = message.body as EMImageMessageBody
        val imgUri = body.localUri
        CommonUtil.takePersistableUriPermission(context, imgUri)
        if(CommonUtil.isFileExistByUri(context, imgUri)){
            Glide.with(context).load(imgUri).into(imageView)
        }else{
            downloadImage()
        }
    }

    private fun downloadImage(){
        val msg = context.getString(R.string.download_picture)
        progressDialog.setMessage(msg)
        progressDialog.show()
        message.setMessageStatusCallback(object: EMCallBack {
            override fun onSuccess() {
                EMLog.e(TAG, "onSuccess")
                progressDialog.dismiss()
                ThreadManager.instance.runOnMainThread{
                    val body = message.body as EMImageMessageBody
                    val imgUri = body.localUri
                    Glide.with(context).load(imgUri).into(imageView)
                }
            }

            override fun onError(code: Int, error: String?) {
                EMLog.e(TAG, "onError")
                ThreadManager.instance.runOnMainThread{
                    imageView.setImageResource(R.mipmap.default_img)
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

        EMClient.getInstance().chatManager().downloadAttachment(message)
    }

    fun loadImage(message: EMMessage){
        this.message = message
        setUpView()
    }

}