package io.agora.edu.core.internal.education.api.board

import io.agora.edu.core.internal.framework.data.EduCallback
import io.agora.edu.core.internal.education.api.board.data.EduBoardInfo
import io.agora.edu.core.internal.education.api.board.data.EduBoardRoom
import io.agora.edu.core.internal.education.api.board.listener.EduBoardEventListener
import io.agora.edu.core.internal.framework.EduUserInfo

abstract class EduBoard {
    lateinit var eduBoardRoom: EduBoardRoom
        protected set

    var eventListener: EduBoardEventListener? = null

    abstract fun followMode(enable: Boolean, callback: EduCallback<Unit>)

    abstract fun grantPermission(user: EduUserInfo, callback: EduCallback<Unit>)

    abstract fun revokePermission(user: EduUserInfo, callback: EduCallback<Unit>)

    abstract fun getBoardInfo(callback: EduCallback<EduBoardInfo>)
}
