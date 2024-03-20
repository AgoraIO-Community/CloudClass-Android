package com.agora.edu.component.helper

import android.text.TextUtils
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.TextPinyinUtil

/**
 * author : felix
 * date : 2022/2/28
 * description :
 */
var pinyinCacheMap = mutableMapOf<String, String>()

fun clearPinyinCacheMap(){
    pinyinCacheMap.clear()
}

@Synchronized
fun sort(listData: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
    if (listData.isEmpty()) {
        return listData
    }

    val list = mutableListOf<AgoraUIUserDetailInfo>()
    list.addAll(listData)

    var coHosts = mutableListOf<AgoraUIUserDetailInfo>()
    val users = mutableListOf<AgoraUIUserDetailInfo>()
    list.forEach {
        if (it.isCoHost) {
            coHosts.add(it)
        } else {
            users.add(it)
        }
    }
    coHosts = sort2(coHosts)
    val list1 = sort2(users)
    coHosts.addAll(list1)
    return coHosts
}

@Synchronized
fun sort2(listData: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
    if (listData.isEmpty()) {
        return listData
    }

    val list = mutableListOf<AgoraUIUserDetailInfo>()
    list.addAll(listData)

    val numList = mutableListOf<AgoraUIUserDetailInfo>()
    val listIterator = list.iterator()
    while (listIterator.hasNext()) {
        val info = listIterator.next()
        if (info == null || TextUtils.isEmpty(info.userName)) {
            continue
        }
        val tmp = info.userName[0]
        if (!TextPinyinUtil.isChinaString(tmp.toString()) && tmp.toInt() in 48..57) {
            numList.add(info)
            listIterator.remove()
        }
    }

    numList.sortWith(object : Comparator<AgoraUIUserDetailInfo> {
        override fun compare(o1: AgoraUIUserDetailInfo?, o2: AgoraUIUserDetailInfo?): Int {
            if (o1 == null) {
                return -1
            }
            if (o2 == null) {
                return 1
            }
            return o1.userName.compareTo(o2.userName)
        }
    })

    list.sortWith(object : Comparator<AgoraUIUserDetailInfo> {
        override fun compare(o1: AgoraUIUserDetailInfo?, o2: AgoraUIUserDetailInfo?): Int {
            if (o1 == null) {
                return -1
            }
            if (o2 == null) {
                return 1
            }

            try {
                val ch1 = getPinyinStr(o1)
                val ch2 = getPinyinStr(o2)
                return ch1.compareTo(ch2)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return -1
        }
    })
    list.addAll(numList)
    return list
}

fun getPinyinStr(info: AgoraUIUserDetailInfo): String {
    var userName = ""
    val py = pinyinCacheMap[info.userName]

    if (!TextUtils.isEmpty(py)) {
        userName = py ?: info.userName
    } else {
        if (TextPinyinUtil.isChinaString(info.userName)) {
            TextPinyinUtil.getPinyin(info.userName).let {
                userName = it
                pinyinCacheMap.put(info.userName, it)
            }
        } else {
            userName = info.userName
            pinyinCacheMap.put(info.userName, info.userName)
        }
    }

    return userName
}