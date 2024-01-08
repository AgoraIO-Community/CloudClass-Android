package io.agora.online.easeim.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import io.agora.online.R
import io.agora.chat.ChatMessage
import io.agora.chat.ImageMessageBody
import io.agora.util.EMLog
import io.agora.util.ImageUtils
import io.agora.util.UriUtils
import java.io.File

const val TAG = "CommonUtil"

object CommonUtil {
    val CHAT_ALL_USER_ID = "0000"
    val CHAT_ALL_USER_NAME = "All"
    var AVATAR_URL = "https://download-sdk.oss-cn-beijing.aliyuncs.com/downloads/IMDemo/avatar/Image1.png"

    /**
     * 隐藏软键盘
     */
    fun hideSoftKeyboard(et: EditText) {
        val inputManager: InputMethodManager =
                et.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
                et.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
        )


}

    /**
     * 显示软键盘
     */
    fun showSoftKeyboard(et: EditText) {
        et.requestFocus()
        val inputManager: InputMethodManager =
                et.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun getLastSubFromUri(fileUri: Uri): String? {
        val uri = fileUri.toString()
        if (!uri.contains("/")) {
            return ""
        }
        val lastIndex = uri.lastIndexOf("/")
        return uri.substring(lastIndex + 1)
    }

    private fun uriStartWithContent(fileUri: Uri): Boolean {
        return "content".equals(fileUri.scheme, ignoreCase = true)
    }

    fun takePersistableUriPermission(
            context: Context?,
            fileUri: Uri?,
    ): Uri? {
        if (context == null || fileUri == null) {
            return null
        }
        //目前只处理scheme为"content"的Uri
        if (!uriStartWithContent(fileUri)) {
            return null
        }
        var intentFlags = 0
        val takeFlags =
                intentFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        var last = getLastSubFromUri(fileUri)
        if (!TextUtils.isEmpty(last))
            return try {
                context.contentResolver.takePersistableUriPermission(fileUri, takeFlags)
                Uri.parse(fileUri.toString())
            } catch (e: SecurityException) {
                EMLog.e("CommonUtil", "takePersistableUriPermission failed e: " + e.message)
                null
            }
        try {
            context.contentResolver.takePersistableUriPermission(
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            EMLog.e("CommonUtil", "takePersistableUriPermission failed e: " + e.message)
            return null
        }
        return fileUri

    }

    fun showImage(
            context: Context,
            imageView: ImageView,
            message: ChatMessage
    ): ViewGroup.LayoutParams {
        val body = message.body as? ImageMessageBody ?: return imageView.layoutParams
        //获取图片的长和宽
        var width = body.width
        var height = body.height
        //获取图片本地资源地址
        var imageUri = body.localUri
        // 获取Uri的读权限
        takePersistableUriPermission(context, imageUri)
        EMLog.e(
                TAG,
                "current show small view big file: uri:" + imageUri + " exist: " + isFileExistByUri(
                        context,
                        imageUri
                )
        )
        if (!isFileExistByUri(context, imageUri)) {
            imageUri = body.thumbnailLocalUri()
            takePersistableUriPermission(context, imageUri)
            EMLog.e(
                    TAG,
                    "current show small view thumbnail file: uri:" + imageUri + " exist: " + isFileExistByUri(
                            context,
                            imageUri
                    )
            )
            if (!isFileExistByUri(context, imageUri)) {
                //context.revokeUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                imageUri = null
            }
        }
        //图片附件上传之前从消息体中获取不到图片的长和宽
        if (width == 0 || height == 0) {
            var options: BitmapFactory.Options? = null
            try {
                options = ImageUtils.getBitmapOptions(context, imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (options != null) {
                width = options.outWidth
                height = options.outHeight
            }
        }
        //获取图片服务器地址
        var thumbnailUrl = body.thumbnailUrl
        if (TextUtils.isEmpty(thumbnailUrl)) {
            thumbnailUrl = body.remoteUrl
        }
        return showImage(context, imageView, imageUri, thumbnailUrl, width, height)
    }

    fun isFileExistByUri(context: Context, fileUri: Uri?): Boolean {
        if (fileUri == null) {
            return false
        }
        val filePath = UriUtils.getFilePath(context, fileUri)
        if (!TextUtils.isEmpty(filePath)) {
            return File(filePath).exists()
        }
        return if (UriUtils.uriStartWithFile(fileUri)) {
            val path = fileUri.path
            val exists = File(path).exists()
            val length = File(path).length()
            EMLog.d(TAG, "file uri exist = $exists file length = $length")
            exists
        } else if (!UriUtils.uriStartWithContent(fileUri)) {
            fileUri.toString().startsWith("/") && File(fileUri.toString()).exists()
        } else {
            val documentFile: DocumentFile? = DocumentFile.fromSingleUri(context, fileUri)
            documentFile != null && documentFile.exists()
        }
    }

    private fun showImage(
            context: Context,
            imageView: ImageView,
            imageUri: Uri?,
            imageUrl: String,
            imgWidth: Int,
            imgHeight: Int
    ): ViewGroup.LayoutParams {
        val maxSize: IntArray = getImageMaxSize(context)
        val maxWidth = maxSize[0]
        val maxHeight = maxSize[1]

        val mRadio = maxWidth * 1.0f / maxHeight
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        var radio: Float = imgWidth * 1.0f / if (imgHeight == 0) 1 else imgHeight
        if (radio == 0f) {
            radio = 1f
        }

        //按原图展示的情况
        if (maxHeight == 0 && maxWidth == 0 /*|| (width <= maxWidth && height <= maxHeight)*/) {
            if (context is Activity && (context.isFinishing || context.isDestroyed)) {
                return imageView.layoutParams
            }
            Glide.with(context).load(
                    imageUri
                            ?: imageUrl
            ).diskCacheStrategy(DiskCacheStrategy.NONE).into(imageView)
            return imageView.layoutParams
        }
        val params = imageView.layoutParams
        //如果宽度方向大于最大值，且宽高比过大,将图片设置为centerCrop类型
        //宽度方向设置为最大值，高度的话设置为宽度的1/2
        if (mRadio / radio < 0.1f) {
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            params.width = maxWidth
            params.height = maxWidth / 2
        } else if (mRadio / radio > 4) {
            //如果高度方向大于最大值，且宽高比过大,将图片设置为centerCrop类型
            //高度方向设置为最大值，宽度的话设置为宽度的1/2
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            params.width = maxHeight / 2
            params.height = maxHeight
        } else {
            //对比图片的宽高比，找到最接近最大值的，其余方向，按比例缩放
            if (radio < mRadio) {
                //说明高度方向上更大
                params.height = maxHeight
                params.width = (maxHeight * radio).toInt()
            } else {
                //宽度方向上更大
                params.width = maxWidth
                params.height = (maxWidth / radio).toInt()
            }
        }
        if (context is Activity && (context.isFinishing || context.isDestroyed)) {
            return params
        }
        Glide.with(context)
                .load(imageUri ?: imageUrl)
                .placeholder(imageView.drawable)
                .apply(
                        RequestOptions()
                                .error(R.mipmap.fcr_default_img)
                )
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .dontAnimate()
                .override(params.width, params.height)
                .into(imageView)
        return params
    }

    private fun getImageMaxSize(context: Context): IntArray {
        val screenInfo: FloatArray = getScreenInfo(context)
        val maxSize = IntArray(2)
//        maxSize[0] = (screenInfo[0] / 3).toInt()
//        maxSize[1] = (screenInfo[0] / 2).toInt()
        maxSize[0] = ScreenUtil.instance.dip2px(context, 100F)
        maxSize[1] = ScreenUtil.instance.dip2px(context, 150F)
        return maxSize
    }

    private fun getScreenInfo(context: Context): FloatArray {
        val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val info = FloatArray(5)
        val dm = DisplayMetrics()
        val m = context.resources.displayMetrics
        manager.defaultDisplay.getMetrics(dm)
        info[0] = m.widthPixels.toFloat()
        info[1] = m.heightPixels.toFloat()
//        info[0] = ScreenUtil.instance.screenWidth.toFloat()
//        info[1] = ScreenUtil.instance.screenHeight.toFloat()
        info[2] = m.densityDpi.toFloat()
        info[3] = m.density
        info[4] = m.scaledDensity
        return info
    }
}