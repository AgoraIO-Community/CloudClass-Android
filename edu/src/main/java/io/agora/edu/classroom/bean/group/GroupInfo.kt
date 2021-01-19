package io.agora.edu.classroom.bean.group


data class GroupInfo(
        var groupUuid: String,
        val groupName: String,
        /*组员id集合*/
        val members: MutableList<String>,
        /*小组内的自定义属性*/
        val groupProperties: MutableMap<String, Any>?
) {
    /*是否是整组上台
    * 注意：这里的整组上台指的是老师点击整组上台按钮之后的状态；而不是组中每个单独的人都在台上的状态*/
    var onStage: Boolean = false
}