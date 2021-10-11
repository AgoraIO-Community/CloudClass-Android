package io.agora.edu.core.internal.education.api.record.listener

import io.agora.edu.core.internal.education.api.record.data.EduRecordInfo

interface EduRecordEventListener {
    fun onRecordStarted(record: EduRecordInfo)

    fun onRecordEnded(record: EduRecordInfo)
}
