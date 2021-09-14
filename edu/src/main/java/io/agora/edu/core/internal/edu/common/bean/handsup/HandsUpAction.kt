package io.agora.edu.core.internal.edu.common.bean.handsup

enum class HandsUpAction(val value: Int) {
    StudentApply(1),
    TeacherAccept(2),
    TeacherReject(3),
    StudentCancel(4),
    StudentExit(5),
    TeacherAbort(6),
    TeacherTimeout(7),
    Carousel(10)
}