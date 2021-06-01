package io.agora.uikit.impl.whiteboard

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import io.agora.uikit.R

class AgoraUIBoardLoadingView : LinearLayout {
    private val tag = "BoardLoadingView"

    private lateinit var cardView: CardView
    private lateinit var loadingIc: AppCompatImageView
    private lateinit var content: AppCompatTextView

    constructor(context: Context?) : super(context) {
        view()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        view()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        view()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        view()
    }

    private fun view() {
        val view = inflate(context, R.layout.agora_board_loading_layout, this)
        cardView = view.findViewById(R.id.cardView)
        loadingIc = view.findViewById(R.id.loading_Img)
        content = view.findViewById(R.id.content)
        visibility = GONE
    }

    override fun setVisibility(visibility: Int) {
        if (visibility == VISIBLE) {
            Glide.with(context).asGif().skipMemoryCache(true)
                    .load(R.drawable.agora_board_loading_img)
                    .listener(object : RequestListener<GifDrawable?> {
                        override fun onLoadFailed(e: GlideException?, model: Any?,
                                                  target: Target<GifDrawable?>?,
                                                  isFirstResource: Boolean): Boolean {
                            return false
                        }

                        override fun onResourceReady(resource: GifDrawable?, model: Any?,
                                                     target: Target<GifDrawable?>?, dataSource: DataSource?,
                                                     isFirstResource: Boolean): Boolean {
                            resource?.setLoopCount(GifDrawable.LOOP_FOREVER)
                            return false
                        }
                    }).into(loadingIc)
        } else {
            Glide.with(context).clear(loadingIc)
        }
        super.setVisibility(visibility)
    }

    fun setContent(isLoading: Boolean) {
        content.post {
            content.text = content.context.resources.getString(if (isLoading)
                R.string.agora_board_loading else R.string.agora_board_reconnect)
        }
    }
}