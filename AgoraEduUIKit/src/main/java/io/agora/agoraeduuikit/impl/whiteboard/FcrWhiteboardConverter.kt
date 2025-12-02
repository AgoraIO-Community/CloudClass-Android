package io.agora.agoraeduuikit.impl.whiteboard

import com.herewhite.sdk.domain.Appliance
import com.herewhite.sdk.domain.Region
import com.herewhite.sdk.domain.ShapeType
import io.agora.agoraeduuikit.impl.whiteboard.bean.FcrBoardRegion
import io.agora.agoraeduuikit.impl.whiteboard.bean.WhiteboardApplianceType

/**
 * author : felix
 * date : 2022/6/8
 * description :
 */
object FcrWhiteboardConverter {
    fun convertStringToRegion(region: String?): Region {
        return when (region) {
            FcrBoardRegion.cn -> {
                Region.cn
            }
            FcrBoardRegion.na -> {
                Region.us
            }
            FcrBoardRegion.eu -> {
                Region.gb_lon
            }
            FcrBoardRegion.ap -> {
                Region.sg
            }
            else -> {
                Region.cn
            }
        }
    }

    fun convertShape(type: WhiteboardApplianceType): ShapeType? {
        return when (type) {
            WhiteboardApplianceType.Star -> {
                ShapeType.Pentagram
            }
            WhiteboardApplianceType.Rhombus -> {
                ShapeType.Rhombus
            }
            WhiteboardApplianceType.Triangle -> {
                ShapeType.Triangle
            }
            else -> null
        }
    }

    fun convertApplianceToString(type: WhiteboardApplianceType): String {
        return when (type) {
            WhiteboardApplianceType.Select -> {
                Appliance.SELECTOR
            }
            WhiteboardApplianceType.PenS -> {
                Appliance.PENCIL
            }
            WhiteboardApplianceType.Pen -> {
                Appliance.PENCIL
            }
            WhiteboardApplianceType.Rect -> {
                Appliance.RECTANGLE
            }
            WhiteboardApplianceType.Circle -> {
                Appliance.ELLIPSE
            }
            WhiteboardApplianceType.Line -> {
                Appliance.STRAIGHT
            }
            WhiteboardApplianceType.Eraser -> {
                Appliance.ERASER
            }
            WhiteboardApplianceType.PENCIL_ERASER -> {
                Appliance.PENCIL_ERASER
            }
            WhiteboardApplianceType.Text -> {
                Appliance.TEXT
            }
            WhiteboardApplianceType.Clicker -> {
                Appliance.CLICKER
            }
            WhiteboardApplianceType.Laser -> {
                Appliance.LASER_POINTER
            }
            WhiteboardApplianceType.Arrow -> {
                Appliance.ARROW
            }
            else -> {
                // 其他教具
                ""
            }
        }
    }
}