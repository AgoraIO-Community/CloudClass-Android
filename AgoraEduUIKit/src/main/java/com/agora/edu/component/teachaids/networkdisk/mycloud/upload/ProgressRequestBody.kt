package com.agora.edu.component.teachaids.networkdisk.mycloud.upload

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ProgressRequestBody : RequestBody {
    private var mFile: File
    private val mPath: String? = null
    private var mMediaType: String
    private var mListener: ProgressCallBack
    private var mEachBufferSize = 1024

    constructor(file: File, mediaType: String, listener: ProgressCallBack) {
        mFile = file
        mMediaType = mediaType
        mListener = listener
    }

    constructor(file: File, mediaType: String, eachBufferSize: Int, listener: ProgressCallBack) {
        mFile = file
        mMediaType = mediaType
        mEachBufferSize = eachBufferSize
        mListener = listener
    }

    override fun contentType(): MediaType? {
        // i want to upload only images
        return mMediaType.toMediaTypeOrNull()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        val fileLength = mFile.length()
        val buffer = ByteArray(mEachBufferSize)
        val `in` = FileInputStream(mFile)
        var uploaded: Long = 0
        try {
            var read: Int
            val handler = Handler(Looper.getMainLooper())
            while (`in`.read(buffer).also { read = it } != -1) {
                // update progress on UI thread
                handler.post(ProgressUpdater(uploaded, fileLength))
                uploaded += read.toLong()
                sink.write(buffer, 0, read)
            }
        } finally {
            `in`.close()
        }
    }

    private inner class ProgressUpdater(private val mUploaded: Long, private val mTotal: Long) :
        Runnable {
        override fun run() {
            mListener.onProgressUpdate((100 * mUploaded / mTotal).toInt())
        }
    }
}