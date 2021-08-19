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

    private var videoWidth = 0
    private var videoHeight = 0
    private var marginHorizontal = 0

    init {
        calculateValues(width)

        videosContainer = LinearLayout(context)
        val videosContainerParams = RelativeLayout.LayoutParams(width, height)
        videosContainerParams.leftMargin = left
        videosContainerParams.topMargin = top
        videosContainer.layoutParams = videosContainerParams
        videosContainer.gravity = Gravity.CENTER
        parent.addView(videosContainer)

        videosLayout = LinearLayout(parent.context)
        val videosLayoutW = ViewGroup.LayoutParams.WRAP_CONTENT
        val videosLayoutParams = LinearLayout.LayoutParams(videosLayoutW, videoHeight)
        videosLayoutParams.topMargin = 0
        videosLayout.layoutParams = videosLayoutParams
        videosLayout.orientation = LinearLayout.HORIZONTAL
        videosContainer.addView(videosLayout)

        teacherVideoWindow = AgoraUIVideoGroup(parent.context,
                eduContext, videosLayout, 0, 0, videoWidth,
            videoHeight, 0, EduContextVideoMode.Single)

        val studentVideoWidth = ViewGroup.LayoutParams.WRAP_CONTENT
        studentsVideoWindow = AgoraUserListVideoLayout(parent.context,
            eduContext, videosLayout, studentVideoWidth, videoHeight,
            0, 0, 0f, componentMargin)
        studentsVideoWindow.show(false)
    }

    private fun calculateValues(width: Int) {
        videoWidth = (width * 176 / 1280f).toInt()
        videoHeight = (videoWidth * 9 / 16f).toInt()
        marginHorizontal = (width * 8 / 1280f).toInt()
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