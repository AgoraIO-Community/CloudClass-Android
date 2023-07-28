package io.agora.agoraeduuikit.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream


/**
 * author : felix
 * date : 2022/5/30
 * description :
 */
object ImageUtils {
    val TAG = "ImageUtils"
    val paint = Paint()

    /**
     * 将bitmap集合上下拼接
     *
     * @return
     */
    fun drawMulti(bitmaps: List<Bitmap>): Bitmap? {
        paint.color = Color.WHITE
        var width = bitmaps[0].width
        var height = bitmaps[0].height
        for (i in 1 until bitmaps.size) {
            if (width < bitmaps[i].width) {
                width = bitmaps[i].width
            }
            height += bitmaps[i].height
        }
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.WHITE) // 绘制白色背景，避免png透明图，显示黑酷爱
        canvas.drawBitmap(bitmaps[0], 0f, 0f, paint)
        var h = 0
        for (j in 1 until bitmaps.size) {
            h += bitmaps[j].height
            canvas.drawBitmap(bitmaps[j], 0f, h.toFloat(), paint)
        }
        return result
    }


    //保存到系统相册并刷新
    //如果是安卓Q以下的，在这个方法之前判断一下有没有存储权限，网上大把轮子
    fun saveToGallery(context: Context, bitmap: Bitmap, imageName: String, listener: (Boolean) -> Unit) {
        //val fileName = "Picture_" + System.currentTimeMillis() + ".jpg"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {//安卓10以下不需要特殊处理
            try {
                val insertImage = MediaStore.Images.Media.insertImage(
                    context.contentResolver,
                    bitmap, imageName, null
                )
                val realPath = getRealPathFromURI(Uri.parse(insertImage), context)//得到绝对地址
                if (realPath == "") {
                    //标记保存失败了
                    listener.invoke(false)
                } else {
                    val file1 = File(realPath)//需要通过这种方式给图片打上时间信息
                    // 最后通知图库更新
                    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file1)))
                }
            } catch (e: FileNotFoundException) {
                Log.i(TAG, "保存到相册失败")
                e.printStackTrace()
                listener.invoke(false)

            } finally {
                listener.invoke(true)
                Log.i(TAG, "保存到相册成功")
            }
        } else {//安卓10以上直接插入系统图库
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DESCRIPTION, "Agora")
            values.put(MediaStore.Images.Media.DISPLAY_NAME, imageName)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.TITLE, "Image.jpg")
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            var outputStream: OutputStream? = null
            try {
                outputStream = context.contentResolver.openOutputStream(uri!!)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream!!.close()
            } catch (e: Exception) {
                Log.i(TAG, "保存到相册失败")
                e.printStackTrace()
                listener.invoke(false)

            } finally {
                listener.invoke(true)
                Log.i(TAG, "保存到相册成功")
            }
        }
    }

    //得到绝对地址,用于给图片标识时间,不然保存下来的图片会是1970年的陈年老图
    private fun getRealPathFromURI(contentUri: Uri, context: Context): String {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = context.contentResolver.query(contentUri, proj, null, null, null)
        cursor?.let {
            val columnIndex: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val fileStr: String = cursor.getString(columnIndex)
            cursor.close()
            return fileStr
        }
        return ""
    }
}