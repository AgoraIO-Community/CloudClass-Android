package io.agora.online.impl.whiteboard

import android.content.Context
import android.graphics.Bitmap
import com.herewhite.sdk.Room
import com.herewhite.sdk.domain.Promise
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.Scene
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.online.R
import io.agora.online.component.toast.AgoraUIToast
import io.agora.online.util.ImageUtils
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

/**
 * author : felix
 * date : 2022/6/1
 * description : 保存白板图片到相册
 */
class FcrWhiteBoardImg(var context: Context) {
    val TAG = "AgoraWhiteBoardFunc"
    var roomName: String? = null
    var isRunning = AtomicBoolean(false)
    var list = CopyOnWriteArrayList<Bitmap>()
    var count = 0
    var index = 0

    fun getImageName(): String {
        return roomName + "_" + TimeUtil.getNowDate() + ".jpg"
    }

    val scenePaths = ArrayList<String>()
    fun saveImgToGallery(room: Room) {
        if (isRunning.get()) {
            LogX.e(TAG,"已经在保存白板图片")
            return
        }
        index = 0
        isRunning.set(true)
        room.getEntireScenes(object : Promise<MutableMap<String, Array<Scene>>> {
            override fun then(t: MutableMap<String, Array<Scene>>?) {
                scenePaths.clear()
                list.clear()

                t?.get("/")?.forEach { // 获取主板的截图
                    LogX.e(TAG,"白板scenePath：" + "/" + it.name)
                    scenePaths.add("/" + it.name)
                }
                count = scenePaths.size

                // 按照顺序下载图片，合并
                getWhiteBoardImage(room, scenePaths[index])

                 /*scenePaths.forEach { scenePath ->
                   room.getSceneSnapshotImage(scenePath, object : Promise<Bitmap> {
                        override fun then(t: Bitmap?) {
                            list.add(t)
                            // 最后一个
                            if (count == list.size) {
                                // 10个为一张图
                                var imagePage = 1
                                var bitList = ArrayList<Bitmap>()
                                val bitMap = HashMap<Int, ArrayList<Bitmap>>()
                                // 0 - imageCount * 10
                                list.forEachIndexed { index, bitmap ->
                                    if (index < imagePage * 10) {
                                        bitList.add(bitmap)
                                        if (index == (imagePage * 10 - 1)) {
                                            bitMap[imagePage] = bitList
                                            // 下一页
                                            imagePage++
                                            bitList = ArrayList<Bitmap>()
                                        } else if (index == (list.size - 1)) {
                                            // 最后一个
                                            bitMap[imagePage] = bitList
                                        }
                                    }
                                }
                                LogX.e(TAG,"白板图片分${bitMap.size}张图存储，总共：$count 张图")

                                val size = bitMap.size
                                var currentIndex = 0
                                // wait
                                bitMap.forEach { map ->
                                    currentIndex++
                                    LogX.e(TAG,"开始合成白板第:${map.key}个图>>>>>>")
                                    val bitmap = ImageUtils.drawMulti(map.value)
                                    if (bitmap == null) {
                                        AgoraUIToast.info(context, textResId = R.string.fcr_savecanvas_tips_save_failed)
                                    } else {
                                        ImageUtils.saveToGallery(context, bitmap, getImageName()) {
                                            if (currentIndex == size) {
                                                if (it) {
                                                    LogX.e(TAG,"白板第:${map.key}个图合成完成")
                                                    AgoraUIToast.info(
                                                        context,
                                                        textResId = R.string.fcr_savecanvas_tips_save_successfully
                                                    )
                                                } else {
                                                    LogX.e(TAG,"白板第:${map.key}个图合成完成失败")
                                                    AgoraUIToast.info(
                                                        context,
                                                        textResId = R.string.fcr_savecanvas_tips_save_failed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                isRunning.set(false)
                            }
                        }

                        override fun catchEx(t: SDKError?) {
                            LogX.e(TAG,"获取白板图片失败：${t?.message}")
                            isRunning.set(false)
                            AgoraUIToast.info(context, textResId = R.string.fcr_savecanvas_tips_save_failed)
                        }
                    })
                }*/
            }

            override fun catchEx(t: SDKError?) {
                LogX.e(TAG,"获取白板路径失败：${t?.message}")
                isRunning.set(false)
                AgoraUIToast.info(context, textResId = R.string.fcr_savecanvas_tips_save_failed)
            }
        })
    }

    fun getWhiteBoardImage(room: Room, scenePath: String) {
        room.getSceneSnapshotImage(scenePath, object : Promise<Bitmap> {
            override fun then(t: Bitmap?) {
                list.add(t)
                // 最后一个
                if (count == list.size) {
                    // 10个为一张图
                    var imagePage = 1
                    var bitList = ArrayList<Bitmap>()
                    val bitMap = HashMap<Int, ArrayList<Bitmap>>()
                    // 0 - imageCount * 10
                    list.forEachIndexed { index, bitmap ->
                        if (index < imagePage * 10) {
                            bitList.add(bitmap)
                            if (index == (imagePage * 10 - 1)) {
                                bitMap[imagePage] = bitList
                                // 下一页
                                imagePage++
                                bitList = ArrayList<Bitmap>()
                            } else if (index == (list.size - 1)) {
                                // 最后一个
                                bitMap[imagePage] = bitList
                            }
                        }
                    }
                    LogX.e(TAG,"白板图片分${bitMap.size}张图存储，总共：$count 张图")

                    val size = bitMap.size
                    var currentIndex = 0
                    // wait
                    bitMap.forEach { map ->
                        currentIndex++
                        LogX.e(TAG,"开始合成白板第:${map.key}个图>>>>>>")
                        val bitmap = ImageUtils.drawMulti(map.value)
                        if (bitmap == null) {
                            AgoraUIToast.info(context, textResId = R.string.fcr_savecanvas_tips_save_failed)
                        } else {
                            ImageUtils.saveToGallery(context, bitmap, getImageName()) {
                                if (currentIndex == size) {
                                    if (it) {
                                        LogX.e(TAG,"白板第:${map.key}个图合成完成")
                                        AgoraUIToast.info(
                                            context,
                                            textResId = R.string.fcr_savecanvas_tips_save_successfully
                                        )
                                    } else {
                                        LogX.e(TAG,"白板第:${map.key}个图合成完成失败")
                                        AgoraUIToast.info(
                                            context,
                                            textResId = R.string.fcr_savecanvas_tips_save_failed
                                        )
                                    }
                                }
                            }
                        }
                    }
                    isRunning.set(false)
                } else {
                    index++
                    getWhiteBoardImage(room, scenePaths[index])
                }
            }

            override fun catchEx(t: SDKError?) {
                LogX.e(TAG,"获取白板图片失败：${t?.message}")
                isRunning.set(false)
                AgoraUIToast.info(context, textResId = R.string.fcr_savecanvas_tips_save_failed)
            }
        })
    }
}