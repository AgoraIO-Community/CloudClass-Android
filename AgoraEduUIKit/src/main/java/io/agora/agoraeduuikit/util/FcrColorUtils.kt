package io.agora.agoraeduuikit.util

import android.graphics.Color

/**
 * author : wf
 * date : 2022/8/3 11:47 上午
 * description :
 */
object FcrColorUtils {
    val DEF_COLOR_INT = Color.parseColor("#0073FF")

    fun toHexEncoding(color: Int): String {
        val sb = StringBuffer()
        var R: String = Integer.toHexString(Color.red(color))
        var G: String = Integer.toHexString(Color.green(color))
        var B: String = Integer.toHexString(Color.blue(color))
        //判断获取到的R,G,B值的长度 如果长度等于1 给R,G,B值的前边添0
        R = if (R.length == 1) "0$R" else R
        G = if (G.length == 1) "0$G" else G
        B = if (B.length == 1) "0$B" else B
        sb.append("#") //0x也可以
        sb.append(R)
        sb.append(G)
        sb.append(B)
        return sb.toString()
    }
}