package com.agora.edu.component.teachaids.networkdisk.mycloud.upload

import com.agora.edu.component.teachaids.networkdisk.mycloud.MyCloudService
import com.hyphenate.easeim.modules.manager.ThreadManager
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.log.LogX
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException


class UploadTask(
    var url :String,
    var filePath: String,
    private var mimeType: String,
    private var callback: UploadCallback?= null
) {

    companion object {
        private val uploadService: MyCloudService =
            RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), MyCloudService::class.java)
    }

    var call: Call<ResponseBody<String>>? = null

    private fun uploadFile(url: String,requestBody: RequestBody): Call<ResponseBody<String>> {
        return uploadService.uploadFile(url, requestBody)
    }

    fun start() {
        val file = File(filePath)
//        //实现上传进度监听
//        val requestFile = ProgressRequestBody(file, mimeType, object : ProgressCallBack {
//            override fun onProgressUpdate(percentage: Int) {
//                callback?.onProgressUpdate(filePath, percentage)
//            }
//
//            override fun onError() {}
//            override fun onFinish() {}
//        })
////        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
//
//        val body: RequestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
//
//        call = uploadFile(url,body)
//        call?.enqueue(object : Callback<ResponseBody<String>?> {
//            override fun onResponse(
//                call: Call<ResponseBody<String>?>,
//                response: Response<ResponseBody<String>?>
//            ) {
//                LogX.d("onProgressUpdate: ${response.body()}====${response.code()}")
//                if (response.body() != null) {
//                    callback?.onSuccess(filePath,response.body())
//                }else{
//                    callback?.onError(filePath,"")
//                }
//            }
//
//            override fun onFailure(call: Call<ResponseBody<String>?>, t: Throwable) {
//                LogX.d("onProgressUpdate: ${t.message}")
//                callback?.onError(filePath,"")
//            }
//
//        })


        ThreadManager.instance.runOnIOThread {
            var code=putImg(url,filePath,mimeType)
            if(code==200){
                callback?.onSuccess(code)
            }else{
                callback?.onError(code,"")
            }
        }
    }

    @Throws(IOException::class)
    fun putImg(uploadUrl: String, localPath: String?,mimeType: String): Int {
        val imageType = mimeType.toMediaTypeOrNull()
        return put(imageType, uploadUrl, localPath)
    }

    @Throws(IOException::class)
    fun put(mediaType: MediaType?, uploadUrl: String, localPath: String?): Int {
        val file = File(localPath)
        val body: RequestBody = file.asRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(uploadUrl)
            .put(body)
            .build()
        val client: OkHttpClient = OkHttpClient.Builder().build()
        val response: okhttp3.Response = client.newCall(request).execute()
        return response.code
    }



    fun cancel(){
        if(call != null && call!!.isCanceled){
            callback =null
            call?.cancel()
        }
    }

}