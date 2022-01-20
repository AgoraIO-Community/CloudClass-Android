package io.agora.agoraeduuikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.context.EduContextVideoMode
import io.agora.agoraeduuikit.impl.AbsComponent
import io.agora.agoraeduuikit.impl.container.AgoraUIConfig
import io.agora.agoraeduuikit.impl.users.AgoraUserListVideoLayoutArt2
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.provider.UIDataProviderListener

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

    private val videosContainer: LinearLayout = LinearLayout(context)
    private val videosLayout: LinearLayout
    private val teacherVideoWindow: AgoraUIVideoGroupArt
    private val studentsVideoWindow: AgoraUserListVideoLayoutArt2

    init {
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
        videosContainer.addView(videosLayout)

        val teacherVideoTop = 0
        val teacherVideoW = AgoraUIConfig.SmallClass.videoListVideoWidth
        val teacherVideoH = videosLayoutH
        teacherVideoWindow = AgoraUIVideoGroupArt(parent.context,
            eduContext, videosLayout, 0, teacherVideoTop, teacherVideoW,
            teacherVideoH, 0, EduContextVideoMode.Single)
//        teacherVideoWindow!!.setContainer(this)

        val studentVideoLeft = componentMargin
        val studentVideoTop = 0
        val studentVideoWidth = ViewGroup.LayoutParams.WRAP_CONTENT
        val studentVideoHeight = AgoraUIConfig.SmallClass.videoListVideoHeight
        studentsVideoWindow = AgoraUserListVideoLayoutArt2(parent.context,
            eduContext, videosLayout, studentVideoWidth, studentVideoHeight, studentVideoLeft,
            studentVideoTop, 0f, componentMargin)
//        studentVideoGroup!!.setContainer(this)
        studentsVideoWindow.show(false)
    }

    fun showTeacher(show: Boolean) {
        teacherVideoWindow.show(show)
    }

//    fun setVisibility(userDetailInfo: AgoraUIUserDetailInfo?) {
//        if (userDetailInfo?.role == AgoraEduContextUserRole.Teacher) {
//            teacherVideoWindow.updateUserInfo(userDetailInfo, true)
//        } else {
//            //放到学生列表
//            studentsVideoWindow.updateUserInfo(userDetailInfo)
//        }
//    }

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

    fun getUiDateProviders(): List<UIDataProviderListener> {
        val list = mutableListOf<UIDataProviderListener>()
        list.add(teacherVideoWindow.uiDataProviderListener)
        list.add(studentsVideoWindow.uiDataProviderListener)
        return list
    }

    override fun setRect(rect: Rect) {
    }
}


