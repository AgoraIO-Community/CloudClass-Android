package io.agora.uikit.impl

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.agora.educontext.EduContextPool
import io.agora.educontext.EduContextVideoMode
import io.agora.uikit.impl.container.AgoraUIConfig
import io.agora.uikit.impl.users.AgoraUserListVideoLayout
import io.agora.uikit.impl.video.AgoraUIVideoGroup

class AgoraUIVideoList(
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
    private val teacherVideoWindow: AgoraUIVideoGroup
    private val studentsVideoWindow: AgoraUserListVideoLayout

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
        val videosLayoutH = AgoraUIConfig.SmallClass.teacherVideoHeight
        val videosLayoutParams = LinearLayout.LayoutParams(videosLayoutW, videosLayoutH)
        videosLayoutParams.topMargin = videosLayoutTop
        videosLayout.layoutParams = videosLayoutParams
        videosLayout.orientation = LinearLayout.HORIZONTAL
        videosContainer.addView(videosLayout)

        val teacherVideoTop = 0
        val teacherVideoW = AgoraUIConfig.SmallClass.teacherVideoWidth
        val teacherVideoH = videosLayoutH
        teacherVideoWindow = AgoraUIVideoGroup(parent.context,
                eduContext, videosLayout, 0, teacherVideoTop, teacherVideoW,
                teacherVideoH, 0, EduContextVideoMode.Single)
//        teacherVideoWindow!!.setContainer(this)

        val studentVideoLeft = componentMargin
        val studentVideoTop = 0
        val studentVideoWidth = ViewGroup.LayoutParams.WRAP_CONTENT
        val studentVideoHeight = AgoraUIConfig.SmallClass.teacherVideoHeight
        studentsVideoWindow = AgoraUserListVideoLayout(parent.context,
                eduContext, videosLayout, studentVideoWidth, studentVideoHeight, studentVideoLeft,
                studentVideoTop, 0f, componentMargin)
//        studentVideoGroup!!.setContainer(this)
        studentsVideoWindow.show(false)
    }

    fun showTeacher(show: Boolean) {
        teacherVideoWindow.show(show)
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