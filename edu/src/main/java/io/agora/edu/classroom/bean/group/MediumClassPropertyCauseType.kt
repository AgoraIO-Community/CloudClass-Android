package io.agora.edu.classroom.bean.group

import io.agora.edu.classroom.bean.PropertyCauseType

object MediumClassPropertyCauseType : PropertyCauseType() {

    /*开关分组*/
    const val SWITCHGROUP = 101

    /*更新分组*/
    const val UPDATEGROUP = 102

    /*开关组内讨论*/
    const val SWITCHINTERACTIN = 103

    /*开关PK*/
    const val SWITCHINTERACTOUT = 104

    /*整组开关音频*/
    const val GROUPMEDIA = 201

    /*整组奖励*/
    const val GROUOREWARD = 202

    /*开关举手功能*/
    const val SWITCHCOVIDEO = 301

    /*开关自动上台*/
    const val SWITCHAUTOCOVIDEO = 302

    /*课堂内的学生名单发生变化*/
    const val STUDENTLISTCHANGED = 401

    /*学生的个人奖励*/
    const val STUDENTREWARD = 402
}