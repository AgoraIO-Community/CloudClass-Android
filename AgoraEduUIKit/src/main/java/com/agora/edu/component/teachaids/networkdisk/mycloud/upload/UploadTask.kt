package com.agora.edu.component.teachaids.networkdisk.mycloud.upload

import com.agora.edu.component.teachaids.networkdisk.mycloud.MyCloudService
import com.hyphenate.easeim.modules.manager.ThreadManager
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
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
import java.util.concurrent.TimeoutException


class UploadTask(
    var url :String,
    var filePath: String,
    private var mimeType: String,
    private var callback: UploadCallback?= null
) {

    companion object {
        private val uploadService: MyCloudService =
            AppRetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), MyCloudService::class.java)
    }

    var call: Call<ResponseBody<String>>? = null

    private fun uploadFile(url: String,requestBody: RequestBody): Call<HttpBaseRes<String>> {
        return uploadService.uploadFile(url=url, body = requestBody)
    }

    fun start() {
        ThreadManager.instance.runOnIOThread {
            var code=putImg(url,filePath,mimeType)
            if(code==200){
                callback?.onSuccess(code)
            }else{
                callback?.onError(code,"")
            }
        }
    }

    fun putImg(uploadUrl: String, localPath: String?,mimeType: String): Int {
        val imageType = mimeType.toMediaTypeOrNull()
        return put(imageType, uploadUrl, localPath)
    }

    fun put(mediaType: MediaType?, uploadUrl: String, localPath: String?): Int {
        try {
            val file = File(localPath)
            val body: RequestBody = file.asRequestBody(mediaType)
            val request: Request = Request.Builder()
                .url(uploadUrl)
                .put(body)
                .build()
            val client: OkHttpClient = OkHttpClient.Builder().build()
            val response: okhttp3.Response = client.newCall(request).execute()
            return response.code
        }catch (e:Exception){
            return -1
        }catch (e1:IOException){
            return -1
        }catch (e2:TimeoutException){
            return -1
        }
    }



    fun cancel(){
        if(call != null && call!!.isCanceled){
            callback =null
            call?.cancel()
        }
    }

}