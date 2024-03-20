package io.agora.agoraeduuikit.impl.whiteboard

import android.content.Context
import android.graphics.Bitmap
import android.text.TextUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.impl.whiteboard.bean.MyProjectorAttributes
import io.agora.agoraeduuikit.impl.whiteboard.bean.MyProjectorOptions
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import java.io.File
import java.util.UUID

/**
 * author : felix
 * date : 2022/6/8
 * description : 加载课件
 */
class FcrCourseWareManager(val boardRoom: BoardRoom) {

    private fun load(param: WindowAppParam, promise: Promise<String>? = null) {
        boardRoom.setWindowApp(param, promise)
    }

    /**
     * 打开课件，白板必须有writeable=true后，才可以打开
     */
    fun loadCourseware(list: MutableList<AgoraEduCourseware>) {
        list.forEach {
            loadCourseware(it)
        }
    }


    fun isImage(ext: String?): Boolean {
        val fileImage = ext?.lowercase()
        if (fileImage == "png" || fileImage == "jpg" || fileImage == "jpeg" || fileImage == "gif") {
            return true
        }
        return false
    }

    /**
     * 打开图片
     */
    fun loadImage(context: Context, imageUrl: String?) {
        Glide.with(context).asBitmap().load(imageUrl).into(object : SimpleTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (resource.height == 0) {
                    resource.height = 200
                }

                if (resource.width == 0) {
                    resource.width = 200
                }
                val scale = resource.width / (resource.height * 1.0)

                val information = ImageInformation()
                information.locked = false
                information.width = 200.0
                information.height = information.width / scale * 1.0
                information.centerX = 0.0
                information.centerY = 0.0

                boardRoom.insertImage(imageUrl, information, object : Promise<String> {
                    override fun then(t: String?) {
                    }

                    override fun catchEx(t: SDKError?) {
                        ToastManager.showShort(t?.message ?: "load image error")
                    }
                })
            }
        })
    }

    /**
     *  打开课件
     */
    fun loadCourseware(courseware: AgoraEduCourseware) {
        val dir = File.separator + courseware.resourceUuid
        var windowAppParam = when {
            courseware.conversion == null -> {
                WindowAppParam.createMediaPlayerApp(courseware.resourceUrl, courseware.resourceName)
            }
            courseware.conversion?.canvasVersion == true -> {
                val scenes = convert(courseware)
                WindowAppParam.createSlideApp(dir, scenes, courseware.resourceName)
            }
            else -> {
                val scenes = convert(courseware)
                WindowAppParam.createDocsViewerApp(dir, scenes, courseware.resourceName)
            }
        }

        if (courseware.version == 4 && courseware.conversion != null && ("pptx" == courseware.ext || "ppt" == courseware.ext)) {
            if ("dynamic" == courseware.conversion?.type) { // 插入新的动态PPT
                val taskUuid = courseware.taskUuid
                val prefix = courseware.taskProgress?.prefix
                if (!TextUtils.isEmpty(taskUuid) && !TextUtils.isEmpty(prefix)) {
                    LogX.e("test", "taskUuid=$taskUuid || prefix=${prefix} || name=${courseware.resourceName}")
                    //windowAppParam = WindowAppParam.createSlideApp(taskUuid, prefix, courseware.resourceName)
                    windowAppParam = createSlideApp(taskUuid, prefix, courseware.resourceName)
                }
            }
        }

        windowAppParam?.let {
            load(param = windowAppParam)
        }
    }

    /**
     * scenePath 相同，就可以避免重复打开，但是加载的时候，会提示重复打开
     *
     * WindowAppParam.createSlideApp(taskUuid, prefix, courseware.resourceName)
     */
    fun createSlideApp(taskUuid: String?, prefixUrl: String?, title: String?): WindowAppParam {
        return WindowAppParam(
            WindowAppParam.KIND_SLIDE,
            MyProjectorOptions("/$taskUuid", title),
            MyProjectorAttributes(taskUuid, prefixUrl)
        )
    }

    /**
     * 转换课件
     */
    private fun convert(courseware: AgoraEduCourseware): Array<Scene> {
        return mutableListOf<Scene>().apply {
            courseware.scenes?.let {
                it.forEachIndexed { _, sceneInfo ->
                    val page = if (sceneInfo.ppt != null) {
                        val ppt = sceneInfo.ppt
                        PptPage(ppt?.src, ppt?.width, ppt?.height)
                    } else null

                    this.add(Scene(sceneInfo.name, page))
                }
            }
        }.toTypedArray()
    }
}