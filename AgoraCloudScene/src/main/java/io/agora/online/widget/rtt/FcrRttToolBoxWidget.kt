package io.agora.online.widget.rtt

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.online.R
import io.agora.online.component.common.IAgoraUIProvider
import io.agora.online.databinding.FcrOnlineToolBoxWidgetContentBinding
import io.agora.online.helper.FcrRttOptionsStatusListener
import io.agora.online.helper.IRttOptions
import io.agora.online.helper.RttOptionsManager
import io.agora.online.helper.RttRecordItem
import io.agora.online.options.AgoraEduOptionsComponent
import io.agora.online.options.AgoraEduRttOptionsComponent
import java.text.MessageFormat

/**
 * 功能作用：Rtt工具箱widget
 * 初始注释时间： 2024/7/1 17:25
 * 创建人：王亮（Loren）
 * 思路：
 * 方法：
 * 注意：
 * 修改人：
 * 修改时间：
 * 备注：
 *
 * @author 王亮（Loren）
 */
class FcrRttToolBoxWidget : AgoraBaseWidget() {
    override val TAG = "FcrRttToolsBoxWidget"

    private var contentView: AgoraRttToolBoxWidgetContent? = null


    fun init(
        container: ViewGroup,
        agoraUIProvider: IAgoraUIProvider, agoraEduOptionsComponent: AgoraEduOptionsComponent, conversionStatusView: ViewGroup,
        subtitleView: AgoraEduRttOptionsComponent,
    ) {
        super.init(container)
        contentView = AgoraRttToolBoxWidgetContent(container, agoraUIProvider, agoraEduOptionsComponent, conversionStatusView, subtitleView)
    }

    override fun release() {
        contentView?.dispose()
        super.release()
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?, keys: MutableList<String>,
        operator: EduBaseUserInfo?,
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        contentView?.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
    }

    /**
     * 重置工具准给他
     */
    fun resetEduRttToolBoxStatus() {
        contentView?.resetStatus()
    }


    internal inner class AgoraRttToolBoxWidgetContent(
        val container: ViewGroup, agoraUIProvider: IAgoraUIProvider,
        agoraEduOptionsComponent: AgoraEduOptionsComponent?, conversionStatusView: ViewGroup?, subtitleView: AgoraEduRttOptionsComponent?,
    ) : IRttOptions {
        private val listener = object : FcrRttOptionsStatusListener() {
            override fun conversionViewReset() {
                super.conversionViewReset()
                agoraEduOptionsComponent?.hiddenRtt()
                conversionStatusView?.visibility = View.GONE
                resetStatus()
            }

            override fun subtitlesViewReset(openSuccess: Boolean) {
                super.subtitlesViewReset(openSuccess)
                agoraEduOptionsComponent?.hiddenRtt()
                if (openSuccess) {
                    subtitleView?.visibility = View.VISIBLE
                } else {
                    subtitleView?.visibility = View.GONE
                }
                resetStatus()
            }

            override fun subtitlesStateChange(toOpen: Boolean) {
                super.subtitlesStateChange(toOpen)
                agoraEduOptionsComponent?.hiddenRtt()
                resetStatus()
            }

            override fun conversionStateChange(toOpen: Boolean) {
                super.conversionStateChange(toOpen)
                agoraEduOptionsComponent?.hiddenRtt()
                resetStatus()
            }

            override fun experienceInfoChange(configAllowUseRtt: Boolean, experienceDefaultTime: Int, experienceReduceTime: Int) {
                super.experienceInfoChange(configAllowUseRtt, experienceDefaultTime, experienceReduceTime)
                subtitleView?.setExperienceInfo(configAllowUseRtt, experienceDefaultTime, experienceReduceTime)
            }

            override fun audioStateNotAllowUse() {
                super.audioStateNotAllowUse()
                subtitleView?.setShowStatusInfo(showProgress = false, showIcon = false,
                    text = container.context.getString(R.string.fcr_dialog_rtt_time_limit_status_not_allow_use))
            }

            override fun audioStateNoSpeaking() {
                super.audioStateNoSpeaking()
                subtitleView?.setShowStatusInfo(showProgress = false, showIcon = false,
                    text = container.context.getString(R.string.fcr_dialog_rtt_subtitles_text_no_one_speaking))
            }

            override fun audioStateNoSpeakingMoreTime() {
                super.audioStateNoSpeakingMoreTime()
                subtitleView?.visibility = View.GONE
            }

            override fun audioStateOpening() {
                super.audioStateOpening()
                subtitleView?.setShowStatusInfo(showProgress = true, showIcon = false,
                    text = container.context.getString(R.string.fcr_dialog_rtt_dialog_subtitles_status_opening))
            }

            override fun audioStateShowSettingHint() {
                super.audioStateShowSettingHint()
                subtitleView?.setShowStatusInfo(showProgress = false, showIcon = false,
                    text = container.context.getString(R.string.fcr_dialog_rtt_dialog_subtitles_status_opening_success_hint))
            }

            override fun audioStateSpeaking() {
                super.audioStateSpeaking()
                subtitleView?.setShowStatusInfo(showProgress = false, showIcon = true,
                    text = container.context.getString(R.string.fcr_dialog_rtt_subtitles_text_listening))
            }

            override fun onMessageChange(recordList: List<RttRecordItem>, currentData: RttRecordItem?) {
                super.onMessageChange(recordList, currentData)
                subtitleView?.setShowTranslatorsInfo(currentData?.userHeader ?: "", currentData?.userName ?: "",
                    currentData?.sourceText ?: "", currentData?.targetText)
            }
        }

        /**
         * Rtt功能的管理
         */
        private val rttOptionsManager: RttOptionsManager by lazy {
            RttOptionsManager(this).also {
                subtitleView?.initView(agoraUIProvider)
                it.initView(agoraUIProvider)
                it.addListener(listener)
            }
        }

        /**
         * 内容视图
         */
        private var binding: FcrOnlineToolBoxWidgetContentBinding =
            FcrOnlineToolBoxWidgetContentBinding.inflate(LayoutInflater.from(container.context), container, true)

        init {
            binding.agoraRttDialogLayout.clipToOutline = true
            binding.agoraRttDialogSubtitles.setOnClickListener {
                if (this.rttOptionsManager.isOpenSubtitles()) {
                    binding.agoraRttDialogSubtitlesIcon.isActivated = false
                    this.rttOptionsManager.closeSubtitles()
                } else {
                    binding.agoraRttDialogSubtitlesIcon.isActivated = true
                    this.rttOptionsManager.openSubtitles()
                }
            }
            binding.agoraRttDialogConversion.setOnClickListener {
                if (this.rttOptionsManager.isOpenConversion()) {
                    binding.agoraRttDialogConversionIcon.isActivated = false
                } else {
                    binding.agoraRttDialogConversionIcon.isActivated = true
                }
                this.rttOptionsManager.openConversion()
            }
            rttOptionsManager.onWidgetRoomPropertiesInit(widgetInfo?.roomProperties)
            resetStatus()
        }

        /**
         * 重置显示状态
         */
        fun resetStatus() {
            val experienceReduceTime = rttOptionsManager.getExperienceReduceTime()
            binding.agoraRttDialogSubtitlesIcon.isActivated = rttOptionsManager.isOpenSubtitles()
            binding.agoraRttDialogConversionIcon.isActivated = rttOptionsManager.isOpenConversion()
            binding.fcrOnlineEduRttConversionDialogTimeLimitHint.text = if (experienceReduceTime > 0) {
                MessageFormat.format(container.context.getString(R.string.fcr_dialog_rtt_time_limit_time), experienceReduceTime / 60000)
            } else {
                container.context.getString(R.string.fcr_dialog_rtt_time_limit_end)
            }
        }

        /**
         * 设置是否可以使用RTT功能
         */
        fun setAllowUse(allowUse: Boolean, reduceTime: Int) {
            binding.fcrOnlineEduRttConversionDialogTimeLimitHint.visibility = if (reduceTime > 0) View.VISIBLE else View.GONE
            binding.agoraRttDialogSubtitles.isEnabled = allowUse
            binding.agoraRttDialogConversion.isEnabled = allowUse
            binding.agoraRttDialogSubtitles.alpha = if (allowUse) 1F else 0.8F
            binding.agoraRttDialogConversion.alpha = if (allowUse) 1F else 0.8F
        }

        fun dispose() {
            container.removeView(binding.root)
            rttOptionsManager.removeListener(listener)
        }

        /**
         * 获取应用实例
         */
        override fun getApplicationContext(): Context {
            if (container.context is Activity) {
                return container.context.applicationContext
            }
            return container.context
        }

        /**
         * 当前页面实例
         */
        override fun getActivityContext(): Context {
            return container.context
        }

        /**
         * 判断并切换主线程
         */
        override fun runOnUiThread(runnable: Runnable) {
            (getActivityContext() as Activity).runOnUiThread(runnable)
        }

        /**
         * widget属性信息变更
         */
        fun onWidgetRoomPropertiesUpdated(
            properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?, keys: MutableList<String>,
            operator: EduBaseUserInfo?,
        ) {
            rttOptionsManager.onWidgetRoomPropertiesUpdated(properties, operator)
        }
    }
}