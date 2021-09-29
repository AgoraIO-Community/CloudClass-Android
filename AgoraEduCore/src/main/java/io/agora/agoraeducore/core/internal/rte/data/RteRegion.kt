package io.agora.agoraeducore.core.internal.rte.data

object RteRegion {
    fun rtcRegion(region: String?): Int {
        return when (region) {
            RtcRegion.AREA_NA.name -> RtcRegion.AREA_NA.value
            RtcRegion.AREA_EUR.name -> RtcRegion.AREA_EUR.value
            RtcRegion.AREA_AS.name -> RtcRegion.AREA_AS.value
            else -> RtcRegion.AREA_GLOBAL.value
        }
    }

    fun rtmRegion(region: String?): Int {
        return when (region) {
            RtmRegion.AREA_NA.name -> RtmRegion.AREA_NA.value
            RtmRegion.AREA_EUR.name -> RtmRegion.AREA_EUR.value
            RtmRegion.AREA_AS.name -> RtmRegion.AREA_AS.value
            else -> RtmRegion.AREA_GLOBAL.value
        }
    }
}

enum class RtcRegion(val value: Int) {
    AREA_GLOBAL(-1),
    AREA_NA(2),
    AREA_EUR(4),
    AREA_AS(8)
}

enum class RtmRegion(val value: Int) {
    AREA_GLOBAL(-1),
    AREA_NA(2),
    AREA_EUR(4),
    AREA_AS(8)
}