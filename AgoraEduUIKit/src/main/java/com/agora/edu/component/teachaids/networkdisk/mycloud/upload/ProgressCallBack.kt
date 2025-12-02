package com.agora.edu.component.teachaids.networkdisk.mycloud.upload

interface ProgressCallBack {
    fun onProgressUpdate(percentage: Int)

    fun onError()

    fun onFinish()
}