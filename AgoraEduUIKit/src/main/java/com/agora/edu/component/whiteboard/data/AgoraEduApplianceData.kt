package com.agora.edu.component.whiteboard.data

import android.content.Context
import android.graphics.Color
import com.agora.edu.component.whiteboard.adpater.*
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType


/**
 * 数据
 */
class AgoraEduApplianceData {
    companion object {
        /**
         * 获取对应的图片资源
         */
        fun getToolResImage(type: WhiteboardApplianceType): Int? {
            var resId: Int? = null
            when (type.value) {
                in 1..100 -> {
                    for (info in getListAppliance()) {
                        if (type == info.activeAppliance) {
                            resId = info.iconRes
                            break
                        }
                    }
                }

                in 101..200 -> {
                    for (info in getListTools()) {
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
        fun getListAppliance(): List<AgoraEduApplianceInfo> {
            return mutableListOf(
                AgoraEduApplianceInfo(R.drawable.agora_wb_clicker, WhiteboardApplianceType.Clicker),          // 选择
                AgoraEduApplianceInfo(R.drawable.agora_wb_select, WhiteboardApplianceType.Select),            // 选择区域
                AgoraEduApplianceInfo(R.drawable.agora_wb_pen, WhiteboardApplianceType.Pen),                  // 画笔

                AgoraEduApplianceInfo(R.drawable.agora_wb_text, WhiteboardApplianceType.Text),                // 文字
                AgoraEduApplianceInfo(R.drawable.agora_wb_eraser, WhiteboardApplianceType.Eraser),            // 橡皮擦

                AgoraEduApplianceInfo(R.drawable.agora_wb_clear, WhiteboardApplianceType.WB_Clear),         // 清空白板
                AgoraEduApplianceInfo(R.drawable.agora_wb_pre_arrow_unable, WhiteboardApplianceType.WB_Pre),      // 上一个操作
                AgoraEduApplianceInfo(R.drawable.agora_wb_next_arrow_unable, WhiteboardApplianceType.WB_Next)     // 下一个操作
            )
        }

        /**
         * 答题器，投票器，倒计时，云盘（201-300）
         *
         * 这一期，老师只有云盘和白板开关
         */
        fun getListTools(): List<AgoraEduToolInfo> {
            return mutableListOf(
                //AgoraEduToolInfo(R.drawable.agora_appliance_tp, WhiteboardApplianceType.Tool_Polling),      // 投票器
                //AgoraEduToolInfo(R.drawable.agora_appliance_dt, WhiteboardApplianceType.Tool_Selector),     // 答题器
                //AgoraEduToolInfo(R.drawable.agora_appliance_djs, WhiteboardApplianceType.Tool_CountDown),   // 倒计时
                AgoraEduToolInfo(R.drawable.agora_appliance_cloud, WhiteboardApplianceType.Tool_Cloud),       // 云盘
                AgoraEduToolInfo(R.drawable.agora_appliance_white_board_switch, WhiteboardApplianceType.Tool_WhiteBoard_Switch)  // 白板开关
            )
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
    }
}












