package io.agora.edu.classroom.bean.group

/**
 * 用户在小组内的属性*/
class UserPropertiesInGroup(
        val groupUuid: String,
        val userGroupProperties: MutableMap<String, Any>,
        val createTime: Long
) {
    companion object {
        const val GROUP = "group"
    }
}