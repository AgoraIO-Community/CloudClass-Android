package com.agora.edu.component.helper

import android.text.TextUtils
import io.agora.agoraeduuikit.provider.AgoraUIUserDetailInfo
import io.agora.agoraeduuikit.util.TextPinyinUtil

/**
 * author : hefeng
 * date : 2022/2/28
 * description :
 */
fun sort(list: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
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

fun sort2(list: MutableList<AgoraUIUserDetailInfo>): MutableList<AgoraUIUserDetailInfo> {
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
            var ch1 = ""
            if (TextPinyinUtil.isChinaString(o1.userName)) {
                TextPinyinUtil.getPinyin(o1.userName).let {
                    ch1 = it
                }
            } else {
                ch1 = o1.userName
            }
            var ch2 = ""
            if (TextPinyinUtil.isChinaString(o2.userName)) {
                TextPinyinUtil.getPinyin(o2.userName).let {
                    ch2 = it
                }
            } else {
                ch2 = o2.userName
            }
            return ch1.compareTo(ch2)
        }
    })
    list.addAll(numList)
    return list
}