package io.agora.online.component.teachaids.networkdisk

import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.online.component.teachaids.networkdisk.Statics.configKey
import io.agora.online.component.teachaids.networkdisk.Statics.publicResourceKey
import io.agora.online.component.teachaids.networkdisk.mycloud.FCRCloudDiskMyCloudFragment
import com.google.android.material.tabs.TabLayout
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetInfo
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineCloudDiskTabItemLayoutBinding
import io.agora.online.databinding.FcrOnlineCloudDiskWidgetContentBinding
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.online.impl.whiteboard.bean.AgoraBoardInteractionSignal.*

/**
 * author : cjw
 * date : 2022/3/15
 * description : 网盘widget，本身只负责内容初始化和消息传递，所有UI具体实现都在 AgoraEduNetworkDiskWidgetContent 中
 * networkDiskWidget control  init widgetContent and transmit msg, but AgoraEduNetworkDiskWidgetContent implement all function and UI
 */
class FCRCloudDiskWidget : AgoraBaseWidget() {
    override val TAG = "AgoraEduNetworkDiskWidget"

    private var cloudDiskContent: AgoraEduCloudDiskWidgetContent? = null

    override fun init(container: ViewGroup) {
        super.init(container)
        container.post {
            widgetInfo?.let {
                cloudDiskContent = AgoraEduCloudDiskWidgetContent(container, it)
            }
        }
    }

    override fun release() {
        cloudDiskContent?.dispose()
        super.release()
    }

    /**
     * author : cjw
     * date : 2022/3/15
     * description : 网盘具体实现
     * networkDisk implementation
     */
    private inner class AgoraEduCloudDiskWidgetContent(val container: ViewGroup, val widgetInfo: AgoraWidgetInfo) :
        FCRCloudCoursewareLoadListener {
        private val tag = "AgoraEduNetworkDiskWidgetContent"

        private val binding = FcrOnlineCloudDiskWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )
        private val tabs = arrayOf(
            container.context.getString(R.string.fcr_cloud_public_resource),
            container.context.getString(R.string.fcr_cloud_private_resource)
        )
        private val tabBindings = mutableListOf<FcrOnlineCloudDiskTabItemLayoutBinding>()
        private val fragments = mutableMapOf<String, FCRCloudDiskResourceFragment>()
        private var extraMap: Map<String, Any>? = null

        init {
            extraMap = widgetInfo.extraInfo as? Map<String, Any>
            initUI()
        }

        private fun initUI() {
            initTab()
            binding.closeImg.setOnClickListener {
                hideSelf()
            }
        }

        private fun initTab() {
            val publicTab = binding.tabLayout.newTab()
            publicTab.setCustomView(R.layout.fcr_online_cloud_disk_tab_item_layout)
            (publicTab.customView?.findViewById(R.id.title) as? AppCompatTextView)?.text = tabs[0]
            binding.tabLayout.addTab(publicTab)
            publicTab.customView?.let {
                tabBindings.add(FcrOnlineCloudDiskTabItemLayoutBinding.bind(it))
            }
            val mineTab = binding.tabLayout.newTab()
            mineTab.setCustomView(R.layout.fcr_online_cloud_disk_tab_item_layout)
            (mineTab.customView?.findViewById(R.id.title) as? AppCompatTextView)?.text = tabs[1]
            binding.tabLayout.addTab(mineTab)
            mineTab.customView?.let {
                tabBindings.add(FcrOnlineCloudDiskTabItemLayoutBinding.bind(it))
            }
            binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    onSelected(tab)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            updateTabTextStyle(tabBindings[0], false)
                        }
                        1 -> {
                            updateTabTextStyle(tabBindings[1], false)
                        }
                    }
                }

                private fun onSelected(tab: TabLayout.Tab) {
                    when (tab.position) {
                        0 -> {
                            replaceTab(0)
                            updateTabTextStyle(tabBindings[0], true)
                        }
                        1 -> {
                            replaceTab(1)
                            updateTabTextStyle(tabBindings[1], true)
                        }
                    }
                }

                private fun replaceTab(pos: Int) {
                    val fragmentManager = (container.context as? AppCompatActivity)?.supportFragmentManager
                    var tag = ""
                    val fragment: FCRCloudDiskResourceFragment = when (pos) {
                        0 -> {
                            val coursewares =
                                extraMap?.get(publicResourceKey) as? ArrayList<AgoraEduCourseware> ?: ArrayList()
                            tag = FCRCloudDiskPublicResourceFragment.tagStr
                            (fragments[tag]
                                ?: FCRCloudDiskPublicResourceFragment.create(coursewares)) as FCRCloudDiskResourceFragment
                        }
                        else -> {
                            val configPair = extraMap?.get(configKey) as? Pair<String, String>
                            tag = FCRCloudDiskMyCloudFragment.tagStr
                            (fragments[tag]
                                ?: FCRCloudDiskMyCloudFragment.create(configPair)) as FCRCloudDiskResourceFragment
                        }
                    }
                    fragment.coursewareLoadListener = this@AgoraEduCloudDiskWidgetContent
                    fragments[tag] = fragment
                    val fragmentTransAction = fragmentManager?.beginTransaction()
                    fragmentTransAction?.replace(binding.cloudDiskFragmentContainer.id, fragment, tag)
                    fragmentTransAction?.commitNow()
                }

                private fun updateTabTextStyle(binding: FcrOnlineCloudDiskTabItemLayoutBinding, selected: Boolean) {
                    binding.indicatorLine.visibility = if (selected) VISIBLE else INVISIBLE
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    onSelected(tab)
                }
            })
            binding.tabLayout.setSelectedTabIndicator(0)
            binding.tabLayout.selectTab(publicTab)
        }

        private fun hideSelf() {
            (binding.root.parent as? View)?.visibility = GONE
            //todo 关闭云盘，下次进教室不要打开
//            es?.widgetContext()?.setWidgetInActive(widgetId = id, isRemove = true)
        }

        fun dispose() {
            fragments.clear()
            ContextCompat.getMainExecutor(binding.root.context).execute {
                container.removeView(binding.root)
            }
        }

        override fun onLoad(courseware: AgoraEduCourseware) {
            closeCloudWidget()

            if (courseware.ext.equals(AgoraEduCoursewareExt.alf.name)) {
                loadAlfFile(courseware)
            } else {
                val packet = AgoraBoardInteractionPacket(LoadCourseware, courseware)
                GsonUtil.toJson(packet)?.let {
                    sendMessage(it)
                }
            }
        }

        fun closeCloudWidget() {
            binding.closeImg.performClick()
        }

        private fun loadAlfFile(courseware: AgoraEduCourseware) {
            val packet = AgoraBoardInteractionPacket(
                LoadAlfFile,
                courseware//发送打开webview widget的消息
            )
            val json = GsonUtil.toJson(packet)//把课件消息发送给对应的widget
            json?.let { sendMessage(it) }
        }
    }
}