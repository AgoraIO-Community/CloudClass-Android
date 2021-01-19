package io.agora.edu.classroom.bean.group

import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.data.EduUserInfo

class RoomGroupInfo() {
    companion object {
        const val GROUPSTATES = "groupStates"
        const val INTERACTOUTGROUPS = "interactOutGroups"
        const val G1 = "g1"
        const val G2 = "g2"
        const val GROUPS = "groups"
        const val STUDENTS = "students"
        const val GROUPUUID = "groupUuid"
        const val USERUUID = "userUuid"
    }

    /*分组状态*/
    var groupStates: GroupStateInfo? = null

    /*参与组外互动的小组id集合*/
    var interactOutGroups: MutableMap<String, String>? = null

    /*分组后的各小组信息集合*/
    var groups: MutableList<GroupInfo>? = null

    /*班级的全体学生名单(包含在线和不在线)*/
    var allStudent: MutableList<GroupMemberInfo>? = null

    fun updateInteractOutGroups(data: MutableMap<String, String>?) {
        if (data != null) {
            interactOutGroups = mutableMapOf()
            data[G1]?.let {
                interactOutGroups!!.put(G1, it)
            }
            data[G2]?.let {
                interactOutGroups!!.put(G2, it)
            }
        } else {
            interactOutGroups = null
        }
    }

    /**须在updateInteractOutGroups字后调用，保证整组上台状态的正确*/
    fun updateGroups(data: MutableMap<String, GroupInfo>?) {
        if (data != null) {
            groups = mutableListOf()
            val iterable = data.entries.iterator()
            while (iterable.hasNext()) {
                val element = iterable.next()
                val groupInfo = element.value
                groupInfo.groupUuid = element.key
                if (interactOutGroups == null) {
                    groupInfo.onStage = false
                } else {
                    groupInfo.onStage = interactOutGroups!!.containsValue(groupInfo.groupUuid)
                }
                groups!!.add(groupInfo)
            }
        } else {
            groups = null
        }
    }

    fun updateAllStudent(data: MutableMap<String, GroupMemberInfo>?,
                         onlineUsers: MutableList<EduUserInfo>,
                         streams: MutableList<EduStreamInfo>) {
        val onlineUserIds = mutableListOf<String>()
        onlineUsers.forEach {
            onlineUserIds.add(it.userUuid)
        }
        val onStageUserIds = mutableListOf<String>()
        streams?.forEach {
            onStageUserIds.add(it.publisher.userUuid)
        }
        data?.let {
            allStudent = mutableListOf()
            val iterable = it.entries.iterator()
            while (iterable.hasNext()) {
                val element = iterable.next()
                val memberInfo = element.value
                memberInfo.uuid = element.key
                if (onStageUserIds.contains(memberInfo.uuid)) {
                    memberInfo.onStage()
                    memberInfo.enableVideo = streams[onStageUserIds.indexOf(memberInfo.uuid)].hasVideo
                    memberInfo.enableAudio = streams[onStageUserIds.indexOf(memberInfo.uuid)].hasAudio
                }
                /*单独判断当前用户是否在线*/
                memberInfo.online = onlineUserIds.contains(memberInfo.uuid)
                allStudent!!.add(memberInfo)
            }
        }
    }

    /**根据userUuid尝试获取当前所在的组，不属于任何组则返回空*/
    fun getGroupIdByUser(userUuid: String): String? {
        groups?.forEach {
            if (it.members.contains(userUuid)) {
                return it.groupUuid
            }
        }
        return null
    }

    /**根据用户的uuid获取用户的奖励*/
    fun getStudentReward(uuid: String): Int {
        var reward = 0
        allStudent?.forEach {
            if (it.uuid == uuid) {
                reward += it.reward
            }
        }
        return reward
    }

    /**是否开启分组*/
    fun isEnableGroup(): Boolean {
        groupStates?.let {
            return it.state == GroupState.ENABLE.value
        }
        return false
    }

    /**是否开启PK*/
    fun isEnablePK(): Boolean {
        groupStates?.let {
            return isEnableGroup() && it.interactOutGroup == InteractState.ENABLE.value
        }
        return false
    }

    /**
     * 用户上台
     */
    fun membersOnStage(streamEvents: List<EduStreamEvent>) {
        allStudent?.forEach {
            for ((streamInfo) in streamEvents) {
                if (it.uuid == streamInfo.publisher.userUuid) {
                    it.onStage()
                }
            }
        }
    }

    /**
     * 用户下台
     */
    fun membersOffStage(streamEvents: List<EduStreamEvent>) {
        allStudent?.forEach {
            for ((streamInfo) in streamEvents) {
                if (it.uuid == streamInfo.publisher.userUuid) {
                    it.offStage()
                }
            }
        }
    }

    /**用户是否在台上*/
    fun isOnStage(userUuid: String): Boolean {
        allStudent?.forEach {
            if (it.uuid == userUuid) {
                return it.onStage
            }
        }
        return false
    }

    /**用户是是否在名单中
     * 即students列表中是否有当前用户*/
    fun existsInList(userUuid: String): Boolean {
        allStudent?.forEach {
            if (it.uuid == userUuid) {
                return true
            }
        }
        return false
    }

    /**此学生是否存在于台2上(即student.uuid是否存在于g2的组中)*/
    fun existInG2(student: GroupMemberInfo): Boolean {
        if (interactOutGroups != null && interactOutGroups!!.size > 1) {
            val g2Uuid = interactOutGroups!![G2]
            groups?.forEach {
                if (it.groupUuid == g2Uuid) {
                    for (element in it.members) {
                        if (element == student.uuid) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}