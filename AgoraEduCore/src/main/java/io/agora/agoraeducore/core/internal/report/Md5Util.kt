package io.agora.agoraeducore.core.internal.report

import java.math.BigInteger
import java.security.MessageDigest

object Md5Util {
    fun toMD5String(input: String): String {
        return try {
            val md: MessageDigest = MessageDigest.getInstance("MD5")
            md.update(input.toByteArray())
            BigInteger(1, md.digest()).toString(16)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}