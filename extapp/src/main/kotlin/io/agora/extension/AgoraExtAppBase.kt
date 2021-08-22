package io.agora.extension

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.annotation.UiThread
import io.agora.educontext.EduContextPool
import kotlin.math.abs

abstract class AgoraExtAppBase : IAgoraExtApp {
    companion object {
        /**
         * Minimum distances of X and Y directions that can be
         * considered as a view has been dragged.
         * Smaller distances are translated as usually finger
         * shaking when a user touches a view.
         */
        private const val minDistanceX = 8
        private const val minDistanceY = 8
    }

    protected var extAppContext: AgoraExtAppContext? = null
    protected var identifier: String? = null

    private val tag = "AgoraExtAppBase"
    private var engine: AgoraExtAppEngine? = null
    private var draggable = false
    private var parent: RelativeLayout? = null
    private var layout: View? = null

    private var lastPointerId = -1
    private var lastTouchX = -1
    private var lastTouchY = -2
    private var touched = false
    private var hasFixedToCenter = false

    private var shouldSyncPosition = true
    private var shouldReceiveSyncInfo = true
    private var positionInit = false

    @SuppressLint("ClickableViewAccessibility")
    private val moveTouchListener = View.OnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only detect the touch events of the first pointer
                var ignored = false
                if (lastPointerId != -1) {
                    if (lastPointerId != event.getPointerId(0)) {
                        // Current touching pointer is not the pointer of current touch event,
                        // this event will be ignored.
                        ignored = true
                    }
                } else {
                    lastPointerId = event.getPointerId(0)
                }

                if (!ignored) {
                    lastTouchX = event.rawX.toInt()
                    lastTouchY = event.rawY.toInt()
                    touched = true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                var ignored = false
                if (!touched || event.getPointerId(0) != lastPointerId) {
                    ignored = true
                }

                if (!coordinateInRange(event.rawX.toInt(), event.rawY.toInt())) {
                    ignored = true
                }

                if (!ignored) {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    reLayout(x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                Log.d(tag, "on layout touch up or canceled")
                lastPointerId = -1
                lastTouchX = -1
                lastTouchY = -1
                touched = false
            }
        }
        false
    }

    internal fun init(identifier: String, engine: AgoraExtAppEngine) {
        this.engine = engine
        this.identifier = identifier

        engine.getRegisteredExtApp(identifier)?.let { item ->
            AgoraExtAppContext(
                    engine.aPaaSEntry.getRoomInfo(),
                    engine.aPaaSEntry.getLocalUserInfo(),
                    mutableMapOf(), identifier, item.language).let { context ->
                        extAppContext = context
                        engine.aPaaSEntry.getProperties(identifier)?.let { properties ->
                            context.properties.putAll(properties)
                        }
            }
        }
    }

    override fun onExtAppLoaded(context: Context, parent: RelativeLayout,
                                view: View, eduContextPool: EduContextPool?) {
        this.parent = parent
        this.layout = view

        if (!hasFixedToCenter) fixToCenter(true)
    }

    /**
     * Set if the whole app layout responds to drag motion events.
     * The default is not to respond to drag events
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setDraggable(draggable: Boolean) {
        if (this.draggable != draggable) {
            layout?.let {
                this.draggable = draggable
                it.isClickable = this.draggable
                it.setOnTouchListener(if (draggable) moveTouchListener else null)
            }
        }
    }

    private fun coordinateInRange(x: Int, y: Int): Boolean {
        return if (layout == null) {
            false
        } else {
            layout?.let {
                val location = IntArray(2)
                it.getLocationOnScreen(location)
                val layoutX = location[0]
                val layoutY = location[1]
                val layoutW: Int = it.width
                val layoutH: Int = it.height
                layoutX <= x && x <= layoutX + layoutW &&
                        layoutY <= y && y <= layoutY + layoutH
            } ?: false
        }
    }

    private fun reLayout(x: Int, y: Int) {
        layout?.let {
            if (it.parent != parent) {
                return@let
            }

            // Remove center layout rule to be dragged
            if (hasFixedToCenter) {
                fixToCenter(false)
            }

            var diffX: Int = x - lastTouchX
            var diffY: Int = y - lastTouchY

            if (abs(diffX) < minDistanceX && abs(diffY) < minDistanceY) {
                // ignore tiny moves
                diffX = 0
                diffY = 0
            }

            val params = it.layoutParams as ViewGroup.MarginLayoutParams
            val width = it.width
            val height = it.height
            var top = params.topMargin
            var left = params.leftMargin
            val parentWidth: Int = parent?.width ?: 0
            val parentHeight: Int = parent?.height ?: 0

            if (diffX < 0) {
                if (left + diffX < 0) {
                    left = 0
                    diffX = 0
                } else {
                    left += diffX
                }
            } else {
                if (left + width + diffX > parentWidth) {
                    left = parentWidth - width
                    diffX = 0
                } else {
                    left += diffX
                }
            }
            if (diffY < 0) {
                if (top + diffY < 0) {
                    top = 0
                    diffY = 0
                } else {
                    top += diffY
                }
            } else {
                if (top + height + diffY > parentHeight) {
                    top = parentHeight - height
                    diffY = 0
                } else {
                    top += diffY
                }
            }

            params.leftMargin = left
            params.topMargin = top
            layout?.layoutParams = params
            lastTouchX += diffX
            lastTouchY += diffY

            trySyncAppPosition(left, top)
        }
    }

    /**
     * Attach or remove the rule that aligns the extension
     * app to the center of parent.
     */
    private fun fixToCenter(fix: Boolean) {
        parent?.let { parent ->
            layout?.let { layout ->
                if (layout.parent == parent) {
                    val param = layout.layoutParams as RelativeLayout.LayoutParams
                    if (fix) {
                        param.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
                    } else {
                        // Remove rule but needs to keep layout in center of parent
                        param.removeRule(RelativeLayout.CENTER_IN_PARENT)
                        param.leftMargin = (parent.width - layout.width) / 2
                        param.topMargin = (parent.height - layout.height) / 2
                    }
                    layout.layoutParams = param
                    hasFixedToCenter = fix
                }
            }
        }
    }

    fun enableSendTrack(enabled: Boolean) {
        shouldSyncPosition = enabled
    }

    private fun trySyncAppPosition(x: Int, y: Int) {
        if (shouldSyncPosition) {
            identifier?.let { id ->
                val pair = calculateDistancePercentage(x, y)
                syncPosition(id, pair.first, pair.second)
            }
        }
    }

    private fun calculateDistancePercentage(x: Int, y: Int) : Pair<Float, Float> {
        var pair: Pair<Float, Float>? = null
        layout?.let { self ->
            parent?.let { parent ->
                val diffXPer = x / (parent.width - self.width).toFloat()
                val diffYPer = y / (parent.height - self.height).toFloat()
                pair = Pair(diffXPer, diffYPer)
            }
        }
        return pair ?: Pair(0F, 0F)
    }

    /**
     * Sync the position of current ext app to remote users.
     * Effective distance: the distance an ext app can actually
     * move along x or y axis, taking into account its size,
     * because it cannot move outside its parent container.
     * The absolute value is the width or height of parent layout
     * minus the width or height of the ext app itself.
     * @param identifier identifier of the ext app
     * @param x percentage of effective distance along x axis
     * @param y percentage of effective distance along y axis
     */
    private fun syncPosition(identifier: String, x: Float, y: Float) {
        extAppContext?.let {
            engine?.syncAppPosition(identifier, getLocalUserInfo()?.userUuid ?: "", x, y)
        }
    }

    internal fun onPositionSync(userId: String, x: Float, y: Float) {
        if (!shouldReceiveSyncInfo) {
            return
        }

        if (!positionInit) {
            // This ext app has not initialized a sync position,
            // we should adjust it position in order to keep
            // the same with room setting
            movePositionBySync(x, y)
            return
        }

        // If this is the update I have just sent out, ignore
        if (userId == getLocalUserInfo()?.userUuid) return

        movePositionBySync(x, y)
    }

    private fun movePositionBySync(x: Float, y: Float) {
        layout?.let { self ->
            parent?.let { parent ->
                if (hasFixedToCenter) {
                    fixToCenter(false)
                }

                val param = self.layoutParams as ViewGroup.MarginLayoutParams
                param.leftMargin = ((parent.width - self.width) * x).toInt()
                param.topMargin = ((parent.height - self.height) * y).toInt()
                self.layoutParams = param

                if (!positionInit) positionInit = true
            }
        }
    }

    /**
     * set properties to remote service, and every extApp impl will
     * receive the properties in onPropertiesUpdated callback.
     *
     * @param properties the properties map.The key separating by '.' will split to
     *      multi key of the map when received in nPropertiesUpdated callback.
     * @param cause the reason why properties changed.
     */
    fun updateProperties(properties: MutableMap<String, Any?>,
                         cause: MutableMap<String, Any?>,
                         common: MutableMap<String, Any?>?,
                         callback: AgoraExtAppCallback<String>? = null) {
        extAppContext?.let { context ->
            engine?.updateExtAppProperties(context.appIdentifier, properties, cause, common, callback)
            return@updateProperties
        }

        Log.w(tag, "agora extension app engine does not initialize")
    }

    /**
     * delete properties which set by updateProperties method. The properties after deleting will be
     * received by every extApp impl in onPropertiesUpdated callback.
     *
     * @param propertyKeys the keys of properties.
     * @param cause the reason why properties deleted.
     */
    fun deleteProperties(propertyKeys: MutableList<String>,
                         cause: MutableMap<String, Any?>,
                         callback: AgoraExtAppCallback<String>?) {
        extAppContext?.let { context ->
            engine?.deleteExtAppProperties(context.appIdentifier, propertyKeys, cause, callback)
            return@deleteProperties
        }

        Log.w(tag, "agora extension app engine does not initialize")
    }

    /**
     * finish the extApp and then close it
     */
    @UiThread
    fun unload() {
        extAppContext?.let { context ->
            engine?.stopExtApp(context.appIdentifier)
            return@unload
        }

        Log.w(tag, "agora extension app engine does not initialize the context")
    }

    fun getLocalUserInfo(): AgoraExtAppUserInfo? {
        return extAppContext?.localUserInfo
    }

    fun getRoomInfo(): AgoraExtAppRoomInfo? {
        return extAppContext?.roomInfo
    }

    fun getProperties(): MutableMap<String, Any?>? {
        return extAppContext?.properties
    }

    // extAppContext info updated callback
    open fun onRoomInfoUpdate(roomInfo: AgoraExtAppRoomInfo) {

    }

    open fun onLocalUserInfoUpdate(userInfo: AgoraExtAppUserInfo) {

    }

    open fun onPropertiesUpdate(properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?) {

    }
}

data class AgoraExtAppContext(
        var roomInfo: AgoraExtAppRoomInfo,
        var localUserInfo: AgoraExtAppUserInfo,
        val properties: MutableMap<String, Any?>,
        val appIdentifier: String,
        val language: String)