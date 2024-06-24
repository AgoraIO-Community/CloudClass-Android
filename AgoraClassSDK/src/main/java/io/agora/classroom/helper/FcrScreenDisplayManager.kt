package io.agora.classroom.helper

import android.app.Activity
import android.content.Context
import android.content.Context.DISPLAY_SERVICE
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.view.Display
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import com.agora.edu.component.AgoraEduVideoComponent
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.AgoraEduContextVideoSubscribeLevel
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.classroom.ui.AgoraClassTeacherVideoPresentation


/**
 * 功能作用：屏幕管理类
 * 创建人：王亮（Loren）
 * 思路：
 * 方法：
 * 注意：
 * 修改人：
 * 修改时间：
 * 备注：
 * 使用流程：
 * 1、onCreate()---进行实例初始化
 * 2、RoomHandler-onRoomPropertiesUpdated() -- 监听是否开启双屏的参数变化，然后调用
 *    --agoraClassVideoPresenter?.videoSubscribeLevel -- 更新视频分辨率
 *    --screenDisplayManager.resetShowMoreDisplay -- 调整单双屏显示
 * 3、RoomHandler-onJoinRoomSuccess() -- 监听房间加人成功，然后调用双屏更新，因为初始化视频组件优先于房间的标记信息获取
 *    --agoraClassVideoPresenter?.videoSubscribeLevel -- 更新视频分辨率
 *    --screenDisplayManager.resetShowMoreDisplay -- 调整单双屏显示
 *
 *
 * 1、screenDisplayManager.changeMoreScreenDisplay -- 教师端更新房间标记记录更新房间是否双屏属性
 *
 * @author 王亮（Loren）
 * @param pageContext 当前页面实例
 */
class FcrScreenDisplayManager(private val options: FcrScreenDisplayOptions) {
    companion object {
        /**
         * 是否允许开双屏的key
         */
        const val ROOM_TAG_DUAL_SCREEN_KEY = "allow_open_dual_screen"

        /**
         * 是否显示副屏
         */
        var showSecondDisplay = false
    }

    private val TAG = "ScreenDisplayManager"

    /**
     * 屏幕管理器
     */
    private val displayManager: DisplayManager = options.getApplicationContext().getSystemService(DISPLAY_SERVICE) as DisplayManager

    /**
     * 屏幕改变监听器
     */
    private val displayListener: DisplayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            LogX.d(TAG, "Display added: $displayId")
            options.updateMoreScreenShow()
        }

        override fun onDisplayRemoved(displayId: Int) {
            LogX.d(TAG, "Display removed: $displayId")
            options.updateMoreScreenShow()
        }

        override fun onDisplayChanged(displayId: Int) {
            LogX.d(TAG, "Display changed: $displayId")
        }
    }

    /**
     * 当前显示的教师屏幕模块
     */
    private var currentTeacherVideoPresentation: AgoraClassTeacherVideoPresentation? = null

    /**
     * 非显示在副屏的时候的布局属性
     */
    private var smallShowLayoutParams: ViewGroup.LayoutParams? = null

    /**
     * 显示在副屏的时候的布局属性
     */
    private var moreShowLayoutParams: ConstraintLayout.LayoutParams =
        ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT)

    init {
        // 注册DisplayListener
        displayManager.registerDisplayListener(this.displayListener, null)
    }

    /**
     * 获取屏幕列表
     */
    private fun getDisplayList(): Array<Display> {
        return displayManager.displays
    }

    /**
     * 设置显示副屏
     * @param areaViewGroup 原有的教师端视频流组件的父级容器
     * @param teacherVideoView 教师端视频流组件
     * @param eduContext 总的配置信息
     */
    private fun setShowMoreScreenDisplay(areaViewGroup: LinearLayoutCompat, teacherVideoView: AgoraEduVideoComponent, eduContext: EduContextPool?) {
        //判断新建
        val displayList = getDisplayList()
        if (displayList.size > 1 && showSecondDisplay && (currentTeacherVideoPresentation == null || !currentTeacherVideoPresentation!!.isShowing)) {
            //初始化
            currentTeacherVideoPresentation?.dismiss()
            currentTeacherVideoPresentation = AgoraClassTeacherVideoPresentation(this.options.getActivityContext(), displayList[1])
            //显示副屏
            this.currentTeacherVideoPresentation!!.show()
            //从小屏列表中移除教师视图
            smallShowLayoutParams = teacherVideoView.layoutParams
            areaViewGroup.removeView(teacherVideoView)
            areaViewGroup.visibility = View.GONE
            //将view移动到副屏
            this.currentTeacherVideoPresentation!!.binding.root.addView(teacherVideoView, moreShowLayoutParams)
            //设置高分辨率
            eduContext?.userContext()?.getUserList(AgoraEduContextUserRole.Teacher)?.let {
                if (it.isNotEmpty()) {
                    eduContext.streamContext()?.getStreamList(it[0].userUuid)?.let { streamList ->
                        if (streamList.isNotEmpty()) {
                            eduContext.streamContext()
                                ?.setRemoteVideoStreamSubscribeLevel(streamList[0].streamUuid, AgoraEduContextVideoSubscribeLevel.HIGH)
                        }
                    }
                }
            }
        } else {
            setHideMoreScreenDisplay(areaViewGroup, teacherVideoView, eduContext)
        }
        //设置显示的用户信息
        this.setShowUserInfo(eduContext)
    }

    /**
     * 设置隐藏关闭副屏
     * @param areaViewGroup 原有的教师端视频流组件的父级容器
     * @param teacherVideoView 教师端视频流组件
     * @param eduContext 总的配置信息
     */
    private fun setHideMoreScreenDisplay(areaViewGroup: LinearLayoutCompat, teacherVideoView: AgoraEduVideoComponent, eduContext: EduContextPool?) {
        if (currentTeacherVideoPresentation != null && currentTeacherVideoPresentation!!.isShowing) {
            //从副屏移除视图
            this.currentTeacherVideoPresentation!!.binding.root.removeView(teacherVideoView)
            //将视图添加到小屏
            areaViewGroup.addView(teacherVideoView, 0, smallShowLayoutParams)
            areaViewGroup.visibility = View.VISIBLE
            //调低分辨率
            eduContext?.userContext()?.getUserList(AgoraEduContextUserRole.Teacher)?.let {
                if (it.isNotEmpty()) {
                    eduContext.streamContext()?.getStreamList(it[0].userUuid)?.let { streamList ->
                        if (streamList.isNotEmpty()) {
                            eduContext.streamContext()
                                ?.setRemoteVideoStreamSubscribeLevel(streamList[0].streamUuid, AgoraEduContextVideoSubscribeLevel.LOW)
                        }
                    }
                }
            }
            //关闭副屏
            if (this.currentTeacherVideoPresentation!!.isShowing) {
                this.currentTeacherVideoPresentation!!.hide()
            }
        }
    }

    /**
     * 重置更多屏幕显示，仅学生端生效
     * @param showMore true-进行双屏显示，false-进行单屏显示，同时如果有副屏的话使用镜像模式显示
     * @param areaViewGroup 原有的教师端视频流组件的父级容器
     * @param teacherVideoView 教师端视频流组件
     * @param eduContext 总的配置信息
     */
    fun resetShowMoreDisplay(
        showMore: Boolean, areaViewGroup: LinearLayoutCompat, teacherVideoView: AgoraEduVideoComponent,
        eduContext: EduContextPool?,
    ) {
        showSecondDisplay = showMore
        if (AgoraEduContextUserRole.Student == eduContext?.userContext()?.getLocalUserInfo()?.role) {
            this.options.runOnUiThread {
                try {
                    if (showMore) {
                        setShowMoreScreenDisplay(areaViewGroup, teacherVideoView, eduContext)
                    } else {
                        setHideMoreScreenDisplay(areaViewGroup, teacherVideoView, eduContext)
                    }
                } catch (ignore: Exception) {
                    LogX.e(TAG, ignore.message)
                }
            }
        }
    }

    /**
     * 设置显示的用户信息
     */
    fun setShowUserInfo(eduContext: EduContextPool?) {
        options.runOnUiThread {
            this@FcrScreenDisplayManager.currentTeacherVideoPresentation?.binding?.nameText?.text =
                eduContext?.userContext()?.getUserList(AgoraEduContextUserRole.Teacher)?.let {
                    return@let if (it.isNotEmpty()) {
                        it[0].userName
                    } else {
                        ""
                    }
                }
        }
    }

    /**
     * 释放教师屏幕模块
     */
    fun releaseTeacherVideoPresentation() {
        currentTeacherVideoPresentation?.dismiss()
        currentTeacherVideoPresentation = null
        showSecondDisplay = false
        // 取消注册DisplayListener
        displayManager.unregisterDisplayListener(this.displayListener)
    }

    /**
     * 设置是否允许开启双屏，仅教师端和服务器端可以控制修改
     * @param allow true-允许开启双屏，false-禁止开双屏
     */
    fun changeMoreScreenDisplay(eduContext: EduContextPool?, allow: Boolean) {
        if (AgoraEduContextUserRole.Teacher == eduContext?.userContext()?.getLocalUserInfo()?.role) {
            eduContext.roomContext()?.updateRoomProperties(mutableMapOf(Pair(ROOM_TAG_DUAL_SCREEN_KEY, allow)),
                mutableMapOf(Pair(ROOM_TAG_DUAL_SCREEN_KEY, "change screen")), null)
        }
    }
//
//    /**
//     * 是否显示了双屏
//     */
//    fun isShowMoreScreenDisplay(): Boolean {
//        return this.currentTeacherVideoPresentation != null && this.currentTeacherVideoPresentation!!.isShowing
//    }
}
