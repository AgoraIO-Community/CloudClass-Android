package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.context.EduContextUserDetailInfo
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextVideoMode
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.users.AgoraUserListVideoLayoutArt

class AgoraUIVideoListArt(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        componentMargin: Int,
        componentBorder: Int
) : AbsComponent() {
    private val tag = "AgoraUIVideoList"

    private val videosContainer: LinearLayout
    private val videosLayout: LinearLayout
    private val teacherVideoWindow: AgoraUIVideoGroupArt
    private val studentsVideoWindow: AgoraUserListVideoLayoutArt

    init {
        videosContainer = LinearLayout(context)
        val videosContainerParams = RelativeLayout.LayoutParams(width, height)
        videosContainerParams.leftMargin = left
        videosContainerParams.topMargin = top
        videosContainer.layoutParams = videosContainerParams
        videosContainer.gravity = Gravity.CENTER_HORIZONTAL
        parent.addView(videosContainer)

        videosLayout = LinearLayout(parent.context)
        val videosLayoutTop = componentMargin
        val videosLayoutW = ViewGroup.LayoutParams.WRAP_CONTENT
        val videosLayoutH = AgoraUIConfig.SmallClass.videoListVideoHeight
        val videosLayoutParams = LinearLayout.LayoutParams(videosLayoutW, videosLayoutH)
        videosLayoutParams.topMargin = videosLayoutTop
        videosLayout.layoutParams = videosLayoutParams
        videosLayout.orientation = LinearLayout.HORIZONTAL
        videosContainer.addView(videosLayout)//videosLayout:视频窗口区域，包含老师窗口和学生们的窗口，放在一个LinearLayout中

        val teacherVideoTop = 0
        val teacherVideoW = AgoraUIConfig.SmallClass.videoListVideoWidth
        val teacherVideoH = videosLayoutH
        teacherVideoWindow = AgoraUIVideoGroupArt(parent.context,//teacherVideoWindow加到videosLayout中
                eduContext, videosLayout, 0, teacherVideoTop, teacherVideoW,
                teacherVideoH, 0, EduContextVideoMode.Single)
//        teacherVideoWindow!!.setContainer(this)

        val studentVideoLeft = componentMargin
        val studentVideoTop = 0
        val studentVideoWidth = ViewGroup.LayoutParams.WRAP_CONTENT
        val studentVideoHeight = AgoraUIConfig.SmallClass.videoListVideoHeight
        studentsVideoWindow = AgoraUserListVideoLayoutArt(parent.context,//studentsVideoWindow加到videosLayout中
                eduContext, videosLayout, studentVideoWidth, studentVideoHeight, studentVideoLeft,
                studentVideoTop, 0f, componentMargin)
//        studentVideoGroup!!.setContainer(this)
        studentsVideoWindow.show(false)
    }

    fun showTeacher(show: Boolean) {
        teacherVideoWindow.show(show)//显示老师窗口
    }

    fun setVisibility(visibility: Int, userDetailInfo: EduContextUserDetailInfo?) {
        if (userDetailInfo?.user?.role == AgoraEduContextUserRole.Teacher) {
            teacherVideoWindow.setVisibility(visibility, userDetailInfo)
        } else {
            //放到学生列表

            studentsVideoWindow.setVisibility(visibility, userDetailInfo)
        }
    }

    fun showStudents(show: Boolean) {
        studentsVideoWindow.show(show)
    }

    fun studentsIsShown(): Boolean {
        return studentsVideoWindow.isShown()
    }

    fun getVideosContainerTop(): Int {
        return (videosContainer.layoutParams as ViewGroup.MarginLayoutParams).topMargin
    }

    fun getVideosContainerH(): Int {
        return videosContainer.layoutParams.height
    }

    override fun setRect(rect: Rect) {
    }
}