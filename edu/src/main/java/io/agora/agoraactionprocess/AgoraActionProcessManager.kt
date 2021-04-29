package io.agora.agoraactionprocess

import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import io.agora.base.callback.ThrowableCallback
import io.agora.base.network.ResponseBody
import io.agora.base.network.RetrofitManager

class AgoraActionProcessManager(
        private val processConfig: AgoraActionProcessConfig,
        val actionListener: AgoraActionListener?
) {

    companion object {
        const val TAG = "ActionProcessManager"
    }

    /**解析出process的相关配置*/
    fun parseConfigInfo(roomProperties: Map<String, Any>): MutableList<AgoraActionConfigInfo> {
        val actionConfigInfos: MutableList<AgoraActionConfigInfo> = mutableListOf()
        var map: Map<String?, Any?>? = null
        for ((key, value) in roomProperties.entries) {
            if (key == AgoraActionConfigInfo.PROCESSES) {
                map = value as Map<String?, Any?>
                break
            }
        }
        if (map != null) {
            var processUuid: String
            for ((key) in map) {
                key?.let {
                    processUuid = key
                    if (TextUtils.isEmpty(processUuid)) {
                        return actionConfigInfos
                    }
                    val json: String = Gson().toJson(map[processUuid])
                    val agoraActionConfigInfo = Gson().fromJson(json, AgoraActionConfigInfo::class.java)
                    agoraActionConfigInfo.processUuid = processUuid
                    actionConfigInfos.add(agoraActionConfigInfo)
                }
            }
        }
        return actionConfigInfos
    }

    fun parseActionMsg(msgRes: String) {
        val actionMsgRes = Gson().fromJson<AgoraActionMsgRes>(msgRes, AgoraActionMsgRes::class.java)
        actionMsgRes?.let {
            when (actionMsgRes.action) {
                AgoraActionType.AgoraActionTypeApply.value -> {
                    actionListener?.onApply(actionMsgRes)
                }
                AgoraActionType.AgoraActionTypeInvitation.value -> {
                    actionListener?.onInvite(actionMsgRes)
                }
                AgoraActionType.AgoraActionTypeAccept.value -> {
                    actionListener?.onAccept(actionMsgRes)
                }
                AgoraActionType.AgoraActionTypeReject.value -> {
                    actionListener?.onReject(actionMsgRes)
                }
                AgoraActionType.AgoraActionTypeCancel.value -> {
                    actionListener?.onCancel(actionMsgRes)
                }
                else -> {
                    Log.e(TAG, "invalid action!")
                }
            }
        }
    }

    /**设置流程*/
    fun setupAgoraAction(options: AgoraActionOptions, callback: ThrowableCallback<ResponseBody<String>>) {
        RetrofitManager.instance().getService(processConfig.baseUrl, AgoraActionService::class.java)
                .setupAgoraAction(processConfig.appId, processConfig.roomUuid, options.processUuid,
                        options)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(res)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        callback.onFailure(throwable)
                    }
                }))
    }

    /**删除流程*/
    fun deleteAgoraAction(processUuid: String, callback: ThrowableCallback<ResponseBody<String>>) {
        RetrofitManager.instance().getService(processConfig.baseUrl, AgoraActionService::class.java)
                .deleteAgoraAction(processConfig.appId, processConfig.roomUuid, processUuid)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(res)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        callback.onFailure(throwable)
                    }
                }))
    }

    fun startAgoraAction(options: AgoraStartActionOptions, callback: ThrowableCallback<ResponseBody<String>>) {
        RetrofitManager.instance().getService(processConfig.baseUrl, AgoraActionService::class.java)
                .startAgoraAction(processConfig.appId, processConfig.roomUuid, options.toUserUuid,
                        options.processUuid, options.body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(res)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        callback.onFailure(throwable)
                    }
                }))
    }


    fun stopAgoraAction(options: AgoraStopActionOptions, callback: ThrowableCallback<ResponseBody<String>>) {
        RetrofitManager.instance().getService(processConfig.baseUrl, AgoraActionService::class.java)
                .stopAgoraAction(processConfig.appId, processConfig.roomUuid, options.toUserUuid,
                        options.processUuid, options.body)
                .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                    override fun onSuccess(res: ResponseBody<String>?) {
                        callback.onSuccess(res)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        callback.onFailure(throwable)
                    }
                }))
    }

}