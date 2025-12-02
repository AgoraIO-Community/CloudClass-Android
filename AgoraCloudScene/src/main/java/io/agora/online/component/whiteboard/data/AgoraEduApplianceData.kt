package io.agora.online.component.whiteboard.data

import android.content.Context
import android.graphics.Color
import io.agora.agoraeducore.core.AgoraEduCore
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.core.internal.framework.impl.managers.AgoraWidgetManager
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetInfo
import io.agora.online.R
import io.agora.online.component.whiteboard.adpater.AgoraEduApplianceInfo
import io.agora.online.component.whiteboard.adpater.AgoraEduPenColorInfo
import io.agora.online.component.whiteboard.adpater.AgoraEduPenShapeInfo
import io.agora.online.component.whiteboard.adpater.AgoraEduTextSizeInfo
import io.agora.online.component.whiteboard.adpater.AgoraEduThicknessInfo
import io.agora.online.component.whiteboard.adpater.AgoraEduToolInfo
import io.agora.online.config.FcrUIConfigFactory
import io.agora.online.config.component.FcrNetlessBoardUIConfig
import io.agora.online.impl.whiteboard.AgoraWhiteBoardWidget
import io.agora.online.impl.whiteboard.bean.WhiteboardApplianceType

/**
 * 数据
 */
class AgoraEduApplianceData {
    companion object {
        /**
         * 获取对应的图片资源
         */
        fun getToolResImage(roomType: Int, type: WhiteboardApplianceType): Int? {
            var resId: Int? = null
            when (type.value) {
                in 1..100 -> {
                    for (info in getListAppliance(roomType)) {
                        if (type == info.activeAppliance) {
                            resId = info.iconRes
                            break
                        }
                    }
                }

                in 101..200 -> {
                    for (info in getListTools(roomType)) {
                        if (type == info.activeAppliance) {
                            resId = info.iconRes
                            break
                        }
                    }
                }

                in 201..300 -> {
                    for (info in getListPenShape()) {
                        if (type == info.activeAppliance) {
                            resId = info.iconRes
                            break
                        }
                    }
                }
            }

            return resId
        }

        /**
         * 白板教具（1-100）
         */
        fun getListAppliance(roomType: Int): List<AgoraEduApplianceInfo> {
            val list = ArrayList<AgoraEduApplianceInfo>()
            val uiConfig = getToolBarUIConfig(roomType)

            if (uiConfig.mouse.isVisible) {
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_clicker, R.drawable.agora_wb_clicker_select, WhiteboardApplianceType.Clicker)) // 选择
            }

            if (uiConfig.selector.isVisible) {
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_select, R.drawable.agora_wb_select_select,WhiteboardApplianceType.Select)) // 选择区域
            }

            if (uiConfig.pencil.isVisible) {
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_pen,R.drawable.agora_wb_pen_select, WhiteboardApplianceType.Pen)) // 画笔
            }

            if (uiConfig.text.isVisible) {
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_text,R.drawable.agora_wb_text_select, WhiteboardApplianceType.Text)) // 文字
            }

            if (uiConfig.eraser.isVisible) {
//                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_eraser,R.drawable.agora_wb_eraser_select, WhiteboardApplianceType.Eraser)) // 橡皮擦
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_eraser,R.drawable.agora_wb_eraser_select, WhiteboardApplianceType.PENCIL_ERASER)) // 橡皮擦
            }

            if (uiConfig.clear.isVisible) {
                list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_clear, R.drawable.agora_wb_clear, WhiteboardApplianceType.WB_Clear)) // 清空白板
            }

            list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_pre_disable,R.drawable.agora_wb_pre_enable, WhiteboardApplianceType.WB_Pre))    // 上一个操作
            list.add(AgoraEduApplianceInfo(R.drawable.agora_wb_next_disable, R.drawable.agora_wb_next_enable,WhiteboardApplianceType.WB_Next)) // 下一个操作

            return list

            /*return mutableListOf(
                AgoraEduApplianceInfo(R.drawable.agora_wb_clicker, R.drawable.agora_wb_clicker_select, WhiteboardApplianceType.Clicker),          // 选择
                AgoraEduApplianceInfo(R.drawable.agora_wb_select, R.drawable.agora_wb_select_select,WhiteboardApplianceType.Select),            // 选择区域
                AgoraEduApplianceInfo(R.drawable.agora_wb_pen,R.drawable.agora_wb_pen_select, WhiteboardApplianceType.Pen),                  // 画笔

                AgoraEduApplianceInfo(R.drawable.agora_wb_text,R.drawable.agora_wb_text_select, WhiteboardApplianceType.Text),                // 文字
                AgoraEduApplianceInfo(R.drawable.agora_wb_eraser,R.drawable.agora_wb_eraser_select, WhiteboardApplianceType.Eraser),            // 橡皮擦

                AgoraEduApplianceInfo(R.drawable.agora_wb_clear,R.drawable.agora_wb_clear, WhiteboardApplianceType.WB_Clear),               // 清空白板
                AgoraEduApplianceInfo(R.drawable.agora_wb_pre_disable,R.drawable.agora_wb_pre_enable, WhiteboardApplianceType.WB_Pre),      // 上一个操作
                AgoraEduApplianceInfo(R.drawable.agora_wb_next_disable, R.drawable.agora_wb_next_enable,WhiteboardApplianceType.WB_Next)     // 下一个操作

                //AgoraEduApplianceInfo(R.drawable.agora_wb_pre_arrow_unable, WhiteboardApplianceType.WB_Pre),      // 上一个操作
                //AgoraEduApplianceInfo(R.drawable.agora_wb_next_arrow_unable, WhiteboardApplianceType.WB_Next)     // 下一个操作
            )*/
        }

        /**
         * 答题器，投票器，倒计时，云盘（201-300）
         *
         * 这一期，老师只有云盘和白板开关
         */
        fun getListTools(roomType: Int): List<AgoraEduToolInfo> {
            val list = ArrayList<AgoraEduToolInfo>()
            val uiConfig = FcrUIConfigFactory.getConfig(roomType)
            val boardUIConfig = getToolBarUIConfig(roomType)

            if (uiConfig.cloudStorage.isVisible) {
                list.add(AgoraEduToolInfo(R.drawable.agora_appliance_cloud, WhiteboardApplianceType.Tool_Cloud)) // 云盘
            }

            if (boardUIConfig.save.isVisible) {
                list.add(AgoraEduToolInfo(R.drawable.agora_appliance_wb_img, WhiteboardApplianceType.Tool_WhiteBoard_IMG)) // 白板图片
            }

            if (boardUIConfig.Switch.isVisible) {
                list.add(AgoraEduToolInfo(R.drawable.agora_appliance_wb_close, WhiteboardApplianceType.Tool_WhiteBoard_Close)) // 白板开关
            }

            return list
            /*return mutableListOf(
                //AgoraEduToolInfo(R.drawable.agora_appliance_tp, WhiteboardApplianceType.Tool_Polling),      // 投票器
                //AgoraEduToolInfo(R.drawable.agora_appliance_dt, WhiteboardApplianceType.Tool_Selector),     // 答题器
                //AgoraEduToolInfo(R.drawable.agora_appliance_djs, WhiteboardApplianceType.Tool_CountDown),   // 倒计时
                AgoraEduToolInfo(R.drawable.agora_appliance_cloud, WhiteboardApplianceType.Tool_Cloud),       // 云盘
                //AgoraEduToolInfo(R.drawable.agora_appliance_white_board_switch, WhiteboardApplianceType.Tool_WhiteBoard_Switch),  // 白板开关
                AgoraEduToolInfo(R.drawable.agora_appliance_wb_img, WhiteboardApplianceType.Tool_WhiteBoard_IMG)  // 白板图片
            )*/
        }

        fun getToolBarUIConfig(roomType: Int): FcrNetlessBoardUIConfig {
            return FcrUIConfigFactory.getConfig(roomType).netlessBoard
        }

        /**
         * 笔的形状（101-200）
         */
        fun getListPenShape(): List<AgoraEduPenShapeInfo> {
            return mutableListOf(
                AgoraEduPenShapeInfo(R.drawable.agora_wb_s, WhiteboardApplianceType.PenS),               // S：笔
                AgoraEduPenShapeInfo(R.drawable.agora_wb_line, WhiteboardApplianceType.Line),           // 线
                AgoraEduPenShapeInfo(R.drawable.agora_wb_rect, WhiteboardApplianceType.Rect),           // 矩形
                AgoraEduPenShapeInfo(R.drawable.agora_wb_circle, WhiteboardApplianceType.Circle),       // 圆形

                AgoraEduPenShapeInfo(R.drawable.agora_wb_star, WhiteboardApplianceType.Star),           // 星形
                AgoraEduPenShapeInfo(R.drawable.agora_wb_rhombus, WhiteboardApplianceType.Rhombus),     // 菱形
                AgoraEduPenShapeInfo(R.drawable.agora_wb_arrowhead, WhiteboardApplianceType.Arrow),     // 箭头
                AgoraEduPenShapeInfo(R.drawable.agora_wb_triangle, WhiteboardApplianceType.Triangle)    // 三角形
            )
        }

        /**
         * 文字大小
         */
        fun getListTextSize(context: Context): List<AgoraEduTextSizeInfo> {
            val textValues = context.resources.getIntArray(R.array.agora_board_font_sizes)
            // 文字T icon 大小
            val textViewSizes = mutableListOf(
                context.resources.getDimensionPixelOffset(R.dimen.agora_edu_text_t_1),
                context.resources.getDimensionPixelOffset(R.dimen.agora_edu_text_t_2),
                context.resources.getDimensionPixelOffset(R.dimen.agora_edu_text_t_3),
                context.resources.getDimensionPixelOffset(R.dimen.agora_edu_text_t_4)
            )

            val list = mutableListOf<AgoraEduTextSizeInfo>()

            for ((index, value) in textValues.withIndex()) {
                list.add(AgoraEduTextSizeInfo(R.drawable.agora_wb_t, textViewSizes[index], value))
            }

            return list
        }

        /**
         * 画笔颜色
         */
        fun getListColor(context: Context): List<AgoraEduPenColorInfo> {
            val iconColors = context.resources.getStringArray(R.array.agora_tool_color_plate)

            val list = mutableListOf<AgoraEduPenColorInfo>()
            for (color in iconColors) {
                list.add(AgoraEduPenColorInfo(Color.parseColor(color)))
            }
            return list
        }

        /**
         * 画笔粗细
         */
        fun getListThickness(context: Context): List<AgoraEduThicknessInfo> {
            return mutableListOf(
                AgoraEduThicknessInfo(
                    R.drawable.agora_edu_wb_circle,
                    context.resources.getDimensionPixelSize(R.dimen.agora_c_1),
                    context.resources.getDimensionPixelSize(R.dimen.agora_appliance_item_size_normal),
                    1
                ),
                AgoraEduThicknessInfo(
                    R.drawable.agora_edu_wb_circle,
                    context.resources.getDimensionPixelSize(R.dimen.agora_c_2),
                    context.resources.getDimensionPixelSize(R.dimen.agora_appliance_item_size_normal),
                    2
                ),
                AgoraEduThicknessInfo(
                    R.drawable.agora_edu_wb_circle,
                    context.resources.getDimensionPixelSize(R.dimen.agora_c_3),
                    context.resources.getDimensionPixelSize(R.dimen.agora_appliance_item_size_normal),
                    3
                ),
                AgoraEduThicknessInfo(
                    R.drawable.agora_edu_wb_circle,
                    context.resources.getDimensionPixelSize(R.dimen.agora_c_4),
                    context.resources.getDimensionPixelSize(R.dimen.agora_appliance_item_size_normal),
                    4
                ),
                AgoraEduThicknessInfo(
                    R.drawable.agora_edu_wb_circle,
                    context.resources.getDimensionPixelSize(R.dimen.agora_c_5),
                    context.resources.getDimensionPixelSize(R.dimen.agora_appliance_item_size_normal),
                    5
                ),
            )
        }

        /**
         * 获取当前白板是否有 Writeable 权限
         */
        fun isBoardGrant(widgetInfo: AgoraWidgetInfo?, userUuid: String?): Boolean {
            var isWriteable = false
            try {
                // 避免数据格式不对
                isWriteable = (widgetInfo?.roomProperties?.get(AgoraWidgetManager.grantUser) as? Map<String, Boolean>)?.get(userUuid)?:false
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return isWriteable
        }

        /**
         * 获取所有授权的人
         */
        fun getBoardAllGrantUsers(widgetInfo: AgoraWidgetInfo?): MutableList<String> {
            val list = mutableListOf<String>()
            try {
                val usersMap = widgetInfo?.roomProperties?.get(AgoraWidgetManager.grantUser) as? MutableMap<String, Boolean>
                usersMap?.forEach {
                    if (it.value) {
                        list.add(it.key)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return list
        }

        fun removeMyBoardGrant(whiteBoardWidget: AgoraWhiteBoardWidget?, userUuid: String?) {
            userUuid?.let {
                val grantedUsers = whiteBoardWidget?.widgetInfo?.roomProperties?.get(AgoraWidgetManager.grantUser) as? MutableMap<*, *>
                grantedUsers?.remove(userUuid)
            }
        }

        /**
         * 是否打开白板
         */
        fun isEnableBoard(eduCore: AgoraEduCore?): Boolean{
            val state = ((eduCore?.room()?.roomProperties?.get("widgets") as? Map<*, *>)?.get(AgoraWidgetDefaultId.WhiteBoard.name) as? Map<*, *>)?.get("state")
            if (state != 1) { // 白板权限
                return false
            }
            return true
        }

        fun isOpenBoardWidget(eduCore: AgoraEduCore?): Boolean {
            return isOpenBoardWidget(eduCore?.eduContextPool())
        }

        fun isOpenBoardWidget(eduContext: EduContextPool?): Boolean {
            return eduContext?.widgetContext()?.isWidgetActive(AgoraWidgetDefaultId.WhiteBoard.id) ?: false
        }
    }
}












