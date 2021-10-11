package io.agora.edu.uikit.impl.container

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import io.agora.edu.R
import io.agora.edu.uikit.handlers.RoomHandler
import io.agora.edu.uikit.handlers.WhiteboardHandler

class AgoraDebugContainer(
        eduContext: io.agora.edu.core.context.EduContextPool?,
        configs: AgoraContainerConfig) : AbsUIContainer(eduContext, configs) {

    private val tag = "AgoraDebugContainer"

    private var logTextView: AppCompatTextView? = null
    private var whiteboardContainer: FrameLayout? = null

    private val roomHandler = object : RoomHandler() {
        override fun onClassroomJoinSuccess(roomUuid: String, timestamp: Long) {
            logTextView?.text = "joinRoom success!"
        }
    }

    private val whiteBoardHandler = object : WhiteboardHandler() {
        override fun getBoardContainer(): ViewGroup? {
            return whiteboardContainer
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        LayoutInflater.from(getContext()).inflate(R.layout.debug_container_layout, layout)
        val container = layout.findViewById<FrameLayout>(R.id.container)
        logTextView = layout.findViewById(R.id.log)
        whiteboardContainer = layout.findViewById(R.id.whiteboard_container)
        getEduContext()?.whiteboardContext()?.addHandler(whiteBoardHandler)
        getEduContext()?.roomContext()?.addHandler(roomHandler)

        layout.findViewById<Button>(R.id.openCamera).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.openCamera()
            }
        }

        layout.findViewById<Button>(R.id.closeCamera).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.closeCamera()
            }
        }

        layout.findViewById<Button>(R.id.startPreview).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.startPreview(container)
            }
        }

        layout.findViewById<Button>(R.id.stopPreview).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.stopPreview()
            }
        }

        layout.findViewById<Button>(R.id.openMic).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.openMicrophone()
            }
        }

        layout.findViewById<Button>(R.id.closeMic).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.closeMicrophone()
            }
        }

        layout.findViewById<Button>(R.id.joinRoom).setOnClickListener { view ->
            run {
                getEduContext()?.roomContext()?.joinClassroom()
            }
        }

        layout.findViewById<Button>(R.id.exitRoom).setOnClickListener { view ->
            run {
                getEduContext()?.roomContext()?.leave()
            }
        }

        layout.findViewById<Button>(R.id.publishVideo).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.publishStream(io.agora.edu.core.context.EduContextMediaStreamType.Video)
            }
        }

        layout.findViewById<Button>(R.id.unPublishVideo).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.unPublishStream(io.agora.edu.core.context.EduContextMediaStreamType.Video)
            }
        }

        layout.findViewById<Button>(R.id.publishAudio).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.publishStream(io.agora.edu.core.context.EduContextMediaStreamType.Audio)
            }
        }

        layout.findViewById<Button>(R.id.unPublishAudio).setOnClickListener { view ->
            run {
                getEduContext()?.mediaContext()?.unPublishStream(io.agora.edu.core.context.EduContextMediaStreamType.Audio)
            }
        }
    }

    override fun setFullScreen(fullScreen: Boolean) {

    }

    override fun calculateVideoSize() {

    }

    override fun willLaunchExtApp(appIdentifier: String): Int {
        return 0
    }

    override fun getWhiteboardContainer(): ViewGroup? {
        return whiteboardContainer
    }
}