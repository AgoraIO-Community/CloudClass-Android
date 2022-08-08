package io.agora.agoraeduuikit.impl.whiteboard

import com.herewhite.sdk.domain.*
import io.agora.agoraeducore.core.internal.education.impl.Constants
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeduuikit.impl.whiteboard.netless.manager.BoardRoom
import java.io.File

/**
 * author : hefeng
 * date : 2022/6/8
 * description : 加载课件
 */
class FcrCourseWareManager(val boardRoom: BoardRoom) {
    private var privateCourseWare: AgoraEduCourseware? = null
    private var privateScenes: Array<Scene>? = null

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

    private fun load(param: WindowAppParam, promise: Promise<String>? = null) {
        boardRoom.setWindowApp(param, promise)
    }

    /**
     * Load and display a course ware file in the whiteboard view
     * container.
     * @param dir scene or resource path
     * @param scenes scene array
     * @param title title of the scenes in multi-view mode
     * load private courseware(student's or teacher's local courseware, preset in advance)
     */
    fun loadPrivateCourseWare(promise: Promise<Boolean>? = null) {
        if (privateCourseWare == null) {
            Constants.AgoraLog?.e("Load private courseware fails: no private courseware found.")
            return
        }

        if (privateScenes.isNullOrEmpty()) {
            Constants.AgoraLog?.e("Load private courseware fails: empty private courseware content.")
            return
        }

        val dir = File.separator + privateCourseWare!!.resourceUuid
        val param = WindowAppParam.createDocsViewerApp(dir, privateScenes!!, privateCourseWare!!.resourceName)
        load(param, object : Promise<String> {
            override fun then(t: String?) {
                promise?.then(true)
            }

            override fun catchEx(t: SDKError?) {
                promise?.catchEx(t)
            }
        })
    }

    // load courseware during class，and called at any time by teacher
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
        windowAppParam?.let {
            load(param = windowAppParam)
        }
    }
}