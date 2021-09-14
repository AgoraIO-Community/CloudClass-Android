package io.agora.edu.core.internal.education.impl.user.data.base

import io.agora.edu.core.internal.framework.EduUserEvent
import io.agora.edu.core.internal.framework.EduUserStateChangeType

internal class EduUserStateChangeEvent(
        val event: EduUserEvent,
        val type: EduUserStateChangeType
) {
}