package io.agora.edu.classroom.widget.video

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.utils.widget.MockView
import androidx.constraintlayout.widget.ConstraintLayout
import io.agora.edu.R
import io.agora.edu.classroom.widget.window.AbstractWindow
import io.agora.edu.classroom.widget.window.IMinimizable

class VideoWindow : AbstractWindow, View.OnClickListener {
    private val TAG = "VideoView"
    lateinit var videoContainer: FrameLayout
    lateinit var optionsLayout: ConstraintLayout
    private lateinit var videoImg: AppCompatImageView
    private lateinit var audioView: AudioView
    private lateinit var trophyLayout: LinearLayout
    private lateinit var trophyNumText: AppCompatTextView
    private lateinit var userNameText: AppCompatTextView
    private lateinit var foldImg: AppCompatImageView
    private lateinit var rootLayout: RelativeLayout
    private lateinit var unfoldLayout: RelativeLayout
    private lateinit var videoOffLayout: LinearLayout
    private lateinit var waitTeacherLayout: LinearLayout
    private lateinit var teacherLeaveLayout: LinearLayout
    private lateinit var noCameraLayout: LinearLayout
    private lateinit var foldLayout: RelativeLayout
    private lateinit var userNameTextFold: AppCompatTextView
    private lateinit var unfoldImg: AppCompatImageView

    private var isLocal = true
    private var curState = State.Normal

    constructor(context: Context) : super(context) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        view()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context, attrs, defStyleAttr
    ) {
        view()
    }

    private fun view() {
        inflate(context, R.layout.video_window_layout, this)
        (context as Activity).window.setBackgroundDrawableResource(android.R.color.transparent)
        optionsLayout = findViewById(R.id.options_Layout)
        videoContainer = findViewById(R.id.videoContainer)
        videoContainer.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                child?.let {
                    if (child is TextureView) {
                        setTextureViewRound(child)
                    }
                }
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
            }
        })
        videoImg = findViewById(R.id.ic_video)
        videoImg.setOnClickListener(this)
        audioView = findViewById(R.id.ic_audio)
        audioView.setOnClickListener(this)
        trophyLayout = findViewById(R.id.trophy_Layout)
        trophyNumText = findViewById(R.id.trophyNum_Text)
        userNameText = findViewById(R.id.userName)
        foldImg = findViewById(R.id.ic_fold)
        foldImg.setOnClickListener(this)
        videoOffLayout = findViewById(R.id.videoOff_Layout)
        waitTeacherLayout = findViewById(R.id.waitTeacher_Layout)
        teacherLeaveLayout = findViewById(R.id.teacherLeave_Layout)
        noCameraLayout = findViewById(R.id.noCamera_Layout)
        rootLayout = findViewById(R.id.root_Layout)
        unfoldLayout = findViewById(R.id.unfold_Layout)
        foldLayout = findViewById(R.id.fold_Layout)
        foldLayout.visibility = GONE
        userNameTextFold = findViewById(R.id.userNameText_Fold)
        unfoldImg = findViewById(R.id.unfold_Img)
        unfoldImg.setOnClickListener(this)
        rootLayout.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                rootLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val width = rootLayout.right - rootLayout.left
                val layoutParams = foldLayout.layoutParams
                layoutParams.width = width
                foldLayout.layoutParams = layoutParams
            }
        })
        /**设置默认的弹开和最小化的布局*/
        setLayouts(unfoldLayout, foldLayout)
        /**设置最小化的动画方向*/
        setMinimizeDirection(IMinimizable.Direction.bottom)
    }

    private fun setTextureViewRound(view: View) {
        var radius: Float = context.resources.getDimensionPixelSize(R.dimen.video_window_texture_radius).toFloat()
        val textureOutlineProvider = VideoViewTextureOutlineProvider(radius)
        view.outlineProvider = textureOutlineProvider
        view.clipToOutline = true
    }

    fun init(isLocal: Boolean) {
        this.isLocal = isLocal
        post {
            if (!this.isLocal) {
                videoImg.visibility = GONE
                audioView.isClickable = false
                trophyLayout.visibility = GONE
            } else {
            }
        }
    }

    fun setUserName(name: String) {
        post {
            userNameText.text = name
            userNameTextFold.text = name
        }
    }

    fun update(name: String, hasAudio: Boolean, hasVideo: Boolean) {
        setUserName(name)
        post {
            audioView.isSelected = hasAudio
        }
        updateState(if (hasVideo) State.Normal else State.VideoOff)
    }

    fun updateState(state: State) {
        curState = state
        post {
            videoContainer.visibility = GONE
            waitTeacherLayout.visibility = GONE
            teacherLeaveLayout.visibility = GONE
            videoOffLayout.visibility = GONE
            noCameraLayout.visibility = GONE
            when (state) {
                State.Normal -> {
                    videoContainer.visibility = VISIBLE
                }
                State.WaitTeacher -> {
                    waitTeacherLayout.visibility = VISIBLE
                }
                State.TeacherLeave -> {
                    teacherLeaveLayout.visibility = VISIBLE
                }
                State.VideoOff -> {
                    videoOffLayout.visibility = VISIBLE
                }
                State.NoCamera -> {
                    noCameraLayout.visibility = VISIBLE
                }
            }
        }
    }

    fun updateTrophy(trophy: Int) {
        trophyNumText?.let {
            post {
                if (trophy > 99) {
                    it.text = "x99+"
                } else {
                    it.text = "x".plus(trophy)
                }
            }
        }
    }

    fun updateAudioVolume(volume: Int) {
        audioView?.let {
            post {
                audioView.updateVolume(volume)
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.ic_fold -> {
                Log.e(TAG, "最小化")
                startMinimize()
            }
            R.id.ic_video -> {
            }
            R.id.ic_audio -> {
            }
            R.id.unfold_Img -> {
                Log.e(TAG, "膨胀")
                restoreMinimize()
            }
        }
    }

    fun setMinimizedView(mMinimizedView: View?) {
        setLayouts(unfoldLayout, mMinimizedView)
    }

    enum class State(val value: Int) {
        Normal(0),
        WaitTeacher(1),
        TeacherLeave(2),
        VideoOff(3),
        NoCamera(4)
    }
}