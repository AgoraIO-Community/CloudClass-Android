package io.agora.edu.classroom.bean.group

import io.agora.education.api.stream.data.EduStreamInfo

data class StageStreamInfo(var streamInfo: EduStreamInfo, val groupUuid: String?, var reward: Int) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is StageStreamInfo) {
            return false
        }
        return this.streamInfo == other.streamInfo && this.groupUuid == other.groupUuid &&
                this.reward == other.reward
    }
}