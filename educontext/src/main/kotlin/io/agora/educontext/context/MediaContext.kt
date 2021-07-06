package io.agora.educontext.context

import android.view.ViewGroup
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextMediaStreamType
import io.agora.educontext.eventHandler.IMediaHandler

abstract class MediaContext : AbsHandlerPool<IMediaHandler>() {

    abstract fun startPreview(container: ViewGroup)

    abstract fun stopPreview()

    abstract fun openCamera()

    abstract fun closeCamera()

    abstract fun openMicrophone()

    abstract fun closeMicrophone()

    abstract fun publishStream(type: EduContextMediaStreamType)

    abstract fun unPublishStream(type: EduContextMediaStreamType)

    abstract fun renderRemoteView(container: ViewGroup?, streamUuid: String)
}