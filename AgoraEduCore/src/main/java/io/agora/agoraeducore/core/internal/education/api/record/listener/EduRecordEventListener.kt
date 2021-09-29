package io.agora.agoraeducore.core.internal.education.api.record.listener

import io.agora.agoraeducore.core.internal.education.api.record.data.EduRecordInfo

interface EduRecordEventListener {
    fun onRecordStarted(record: EduRecordInfo)

    fun onRecordEnded(record: EduRecordInfo)
}
