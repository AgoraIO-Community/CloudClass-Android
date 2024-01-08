package com.agora.edu.component.teachaids.networkdisk.mycloud.upload

interface UploadCallback {
    fun onProgressUpdate(path: String?, percentage: Int)

    fun onError(code: Int, error: String?)

    fun onSuccess(code: Int)
}