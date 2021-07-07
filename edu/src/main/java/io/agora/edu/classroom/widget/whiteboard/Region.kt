package io.agora.edu.classroom.widget.whiteboard

import com.herewhite.sdk.domain.Region
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardRegion.ap
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardRegion.cn
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardRegion.eu
import io.agora.edu.classroom.widget.whiteboard.WhiteBoardRegion.na

object RegionUtil {
    fun region(region: String?): Region {
        return when (region) {
            cn -> {
                Region.cn
            }
            na -> {
                Region.us
            }
            eu -> {
                Region.gb_lon
            }
            ap -> {
                Region.sg
            }
            else -> {
                Region.cn
            }
        }
    }
}

object WhiteBoardRegion {
    const val cn = "cn-hz"
    const val na = "us-sv"
    const val eu = "gb-lon"
    const val ap = "sg"
}