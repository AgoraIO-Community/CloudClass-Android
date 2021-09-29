package io.agora.agoraeducore.core.internal.education.impl.user.data.base

import io.agora.agoraeducore.core.internal.framework.EduUserEvent
import io.agora.agoraeducore.core.internal.framework.EduUserStateChangeType

internal class EduUserStateChangeEvent(
        val event: EduUserEvent,
        val type: EduUserStateChangeType
) {
}