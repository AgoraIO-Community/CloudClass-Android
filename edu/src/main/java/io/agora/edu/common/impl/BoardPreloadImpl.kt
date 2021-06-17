package io.agora.edu.common.impl

import android.content.*
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import io.agora.download.DownloadConstant.EXTRA_INTENT_DOWNLOAD
import io.agora.download.DownloadHelper
import io.agora.download.DownloadStatus.*
import io.agora.download.FileInfo
import io.agora.edu.BuildConfig.NETLESS_RESOURCE_CDN_HOST
import io.agora.edu.common.api.BoardPreload
import io.agora.edu.common.bean.board.BoardPreloadFileInfo
import io.agora.edu.common.listener.BoardPreloadListener
import io.agora.edu.util.AppUtil
import io.agora.log.ZipUtils
import java.io.File
import java.io.FileInputStream
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class BoardPreloadImpl(context: Context) : BoardPreload {
    private val TAG = "BoardPreload"

    private var contextWeak = WeakReference<Context>(context)

    private var downloadHelper: DownloadHelper = DownloadHelper.getInstance()

    private var curDestFolder = context.filesDir.absolutePath.plus(File.separatorChar).plus("board")

    private val publicFiles = "publicFiles"

    private val dynamicConvert = "dynamicConvert"

    private val staticConvert = "staticConvert"

    private val zipSuffix = ".zip"

    private val zipSuffixUpperCase = ".ZIP"

    private val listenerMap: MutableMap<String, MutableList<BoardPreloadListener>> = ConcurrentHashMap()
    private lateinit var boardPreloadFileInfoMap: ConcurrentHashMap<String, BoardPreloadFileInfo>
    private val receiverMap: ConcurrentHashMap<String, BroadcastReceiver> = ConcurrentHashMap()

    override fun isAvailable(): Boolean {
        return contextWeak.get() != null
    }

    override fun preload(link: String, listener: BoardPreloadListener?) {
        listener?.let {
            if (this.listenerMap[link] == null) {
                this.listenerMap[link] = mutableListOf()
            }
            if (!this.listenerMap[link]!!.contains(it)) {
                this.listenerMap[link]!!.add(it)
            }
        }
        boardPreloadFileInfoMap = ConcurrentHashMap()
        var destPath = curDestFolder
        destPath = destPath.plus(File.separatorChar)
        when {
            link.contains(publicFiles) -> {
                destPath = destPath.plus(publicFiles)
            }
            link.contains(dynamicConvert) -> {
                destPath = destPath.plus(dynamicConvert)
            }
            link.contains(staticConvert) -> {
                destPath = destPath.plus(staticConvert)
            }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val fileInfo = intent?.getSerializableExtra(EXTRA_INTENT_DOWNLOAD) as FileInfo
                handleProgressEvent(fileInfo, destPath, this)
            }
        }
        receiverMap[link] = receiver
        download(link, destPath, receiver)
    }

    private fun handleProgressEvent(fileInfo: FileInfo, destPath: String, receiver: BroadcastReceiver) {
        val taskAction = buildTaskAction(fileInfo.downloadUrl)
        when (fileInfo.downloadStatus) {
            WAIT -> {
                if (listenerMap.containsKey(fileInfo.downloadUrl)) {
                    listenerMap[fileInfo.downloadUrl]?.forEach {
                        it.onStartDownload(fileInfo.downloadUrl)
                    }
                }
            }
            LOADING -> {
                var progress = fileInfo.downloadLocation.toDouble() / fileInfo.size.toDouble()
                if (listenerMap.containsKey(fileInfo.downloadUrl)) {
                    listenerMap[fileInfo.downloadUrl]?.forEach {
                        it.onProgress(fileInfo.downloadUrl, progress)
                    }
                }
            }
            COMPLETE -> {
                try {
                    contextWeak.get()?.unregisterReceiver(receiver)
                    receiverMap.remove(fileInfo.downloadUrl)
                } catch (e: IllegalArgumentException) {
                }
                boardPreloadFileInfoMap.remove(taskAction)
                object : Thread() {
                    override fun run() {
                        super.run()
                        try {
                            val unZipDirFile = File(fileInfo.filePath.removeSuffix(zipSuffix)
                                    .removeSuffix(zipSuffixUpperCase))
                            if (!unZipDirFile.exists()) {
                                ZipUtils.unZip(File(fileInfo.filePath), destPath)
                            }
                            if (listenerMap.containsKey(fileInfo.downloadUrl)) {
                                listenerMap[fileInfo.downloadUrl]?.forEach {
                                    it.onComplete(fileInfo.downloadUrl)
                                }
                                listenerMap[fileInfo.downloadUrl]?.clear()
                                listenerMap.remove(fileInfo.downloadUrl)
                            }
                        } catch (e: RuntimeException) {
                            e.printStackTrace()
                            fileInfo.downloadStatus = FAIL
                            handleProgressEvent(fileInfo, destPath, receiver)
                        }
                    }
                }.start()
            }
            FAIL -> {
                try {
                    contextWeak.get()?.unregisterReceiver(receiver)
                    receiverMap.remove(fileInfo.downloadUrl)
                } catch (e: IllegalArgumentException) {
                }
                boardPreloadFileInfoMap.remove(taskAction)
                if (listenerMap.containsKey(fileInfo.downloadUrl)) {
                    listenerMap[fileInfo.downloadUrl]?.forEach {
                        it.onFailed(fileInfo.downloadUrl)
                    }
                    listenerMap[fileInfo.downloadUrl]?.clear()
                    listenerMap.remove(fileInfo.downloadUrl)
                }
            }
        }
    }

    private fun download(url: String, destPath: String, receiver: BroadcastReceiver) {
        var fileName = url.split("/")[url.split("/").size - 1]
        if (fileName.endsWith(zipSuffixUpperCase)) {
            fileName = fileName.replace(zipSuffixUpperCase, zipSuffix)
        }
        val file = File(destPath, fileName)
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        if (file.isFile && !file.exists()) {
            file.createNewFile()
        }
        val taskAction = buildTaskAction(url)
        val intentFilter = IntentFilter(taskAction)
        contextWeak.get()?.let {
            it.registerReceiver(receiver, intentFilter)
            boardPreloadFileInfoMap[taskAction] = BoardPreloadFileInfo(url, file, taskAction)
            downloadHelper.addTask(url, file, taskAction).submit(it)
        }
    }

    private fun buildTaskAction(url: String): String {
        return url
    }

    override fun cancelCurPreloadTask(url: String) {
        boardPreloadFileInfoMap.forEach { entry ->
            val info = entry.value
            if (info.url == url) {
                contextWeak.get()?.let {
                    downloadHelper.pauseTask(info.url, info.file, info.action).submit(it)
                }
            }
        }
        try {
            receiverMap.forEach {
                if (it.key == url) {
                    contextWeak.get()?.unregisterReceiver(it.value)
                    receiverMap.remove(it.key)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun cancelAllPreloadTask() {
        try {
            boardPreloadFileInfoMap.forEach { entry ->
                val info = entry.value
                contextWeak.get()?.let {
                    downloadHelper.pauseTask(info.url, info.file, info.action).submit(it)
                }
            }
            receiverMap.forEach {
                try {
                    contextWeak.get()?.unregisterReceiver(it.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                receiverMap.remove(it.key)
            }
            boardPreloadFileInfoMap.clear()
            listenerMap.clear()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun checkCache(request: WebResourceRequest): WebResourceResponse? {
        val host = request.url.host
        if (host == NETLESS_RESOURCE_CDN_HOST) {
            var destPath = curDestFolder
            destPath = destPath.plus(File.separatorChar)
            var url = request.url.toString()
            var segments: List<String>? = null
            when {
                url.contains(publicFiles) -> {
                    segments = url.split(publicFiles)
                    destPath = destPath.plus(File.separatorChar).plus(publicFiles)
                }
                url.contains(dynamicConvert) -> {
                    segments = url.split(dynamicConvert)
                    destPath = destPath.plus(File.separatorChar).plus(dynamicConvert)
                }
                url.contains(staticConvert) -> {
                    segments = url.split(staticConvert)
                    destPath = destPath.plus(File.separatorChar).plus(staticConvert)
                }
            }
            segments?.let {
                url = segments[segments.size - 1]
                val filePath = destPath.plus(File.separatorChar).plus(url)
                val file = File(filePath)
                if (file.exists()) {
                    return try {
                        val mime = AppUtil.getMimeTypeFromUrl(Uri.parse(filePath).toString())
                        val inputStream = FileInputStream(filePath)
                        var response: WebResourceResponse? = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val headers: HashMap<String, String> = HashMap()
                            headers["Access-Control-Allow-Origin"] = "*"
                            headers["Access-Control-Allow-Methods"] = "POST, GET, OPTIONS"
                            headers["Access-Control-Allow-Headers"] = "X-PINGOTHER, Content-Type"
                            val media = url.endsWith("mp4") || url.endsWith("mp3")
                            if (media) {
                                val map: MutableMap<String, String> = request.requestHeaders
                                val tempResponseHeaders: MutableMap<String, String> = HashMap()
                                val totalRange: Int = inputStream.available()
                                val rangeString: String? = map["Range"]
                                rangeString?.let {
                                    val parts = rangeString.split("=".toRegex()).toTypedArray()
                                    val streamParts = parts[1].split("-".toRegex()).toTypedArray()
                                    val fromRange = streamParts[0]
                                    var range = totalRange - 1
                                    if (streamParts.size > 1 && !TextUtils.isEmpty(streamParts[1])) {
                                        range = streamParts[1].toInt()
                                    }
                                    tempResponseHeaders["Accept-Ranges"] = "bytes"
                                    tempResponseHeaders["Content-Range"] = "bytes $fromRange-$range/$totalRange"
                                    val statusCode = if (fromRange === "0") 200 else 206
                                    response = WebResourceResponse(mime, "UTF-8", statusCode, "ok",
                                            tempResponseHeaders, inputStream)
                                }
                            } else {
                                response = WebResourceResponse(mime, "UTF-8", 200, "ok", headers, inputStream)
                            }
                        } else {
                            response = WebResourceResponse(mime, "UTF-8", inputStream)
                        }
                        response
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
            }
        }
        return null
    }
}