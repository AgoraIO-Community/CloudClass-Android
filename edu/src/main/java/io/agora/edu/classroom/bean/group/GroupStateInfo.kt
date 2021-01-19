package io.agora.edu.classroom.bean.group

/**
 * 小组状态信息*/
class GroupStateInfo(
        /*分组是否开启*/
        val state: Int,
        /*组内互动（小组讨论）*/
        val interactInGroup: Int,
        //组外互动（小组pk）(小组讨论状态是进入pk状态的必要条件)
        val interactOutGroup: Int
) {
}