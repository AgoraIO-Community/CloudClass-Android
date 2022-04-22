package com.agora.edu.component.teachaids.networkdisk

import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.LoadCourseware
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.BoardGrantDataChanged
import com.agora.edu.component.teachaids.networkdisk.Statics.configKey
import com.agora.edu.component.teachaids.networkdisk.Statics.publicResourceKey
import com.agora.edu.component.teachaids.networkdisk.mycloud.FCRCloudDiskMyCloudFragment
import com.google.android.material.tabs.TabLayout
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.extensions.widgets.AgoraBaseWidget
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetInfo
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.FcrCloudDiskTabItemLayoutBinding
import io.agora.agoraeduuikit.databinding.FcrCloudDiskWidgetContentBinding
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket

/**
 * author : cjw
 * date : 2022/3/15
 * description : 网盘widget，本身只负责内容初始化和消息传递，所有UI具体实现都在AgoraEduNetworkDiskWidgetContent中
 * networkDiskWidget control  init widgetContent and transmit msg, but AgoraEduNetworkDiskWidgetContent implement all function and UI
 */
class FCRCloudDiskWidget : AgoraBaseWidget() {
    override val tag = "AgoraEduNetworkDiskWidget"

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

        private val binding = FcrCloudDiskWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )
        private val tabs = arrayOf(
            container.context.getString(R.string.fcr_cloud_public_resource),
            container.context.getString(R.string.fcr_cloud_private_resource)
        )
        private val tabBindings = mutableListOf<FcrCloudDiskTabItemLayoutBinding>()
        private val fragments = mutableMapOf<String, FCRCloudDiskResourceFragment>()
        private var extraMap: Map<String, Any>? = null

        val whiteBoardObserver = object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val packet = GsonUtil.jsonToObject<AgoraBoardInteractionPacket>(msg)
                if (packet?.signal?.value == BoardGrantDataChanged.value) {
                    val granted = (packet?.body as ArrayList<*>).contains(widgetInfo.localUserInfo.userUuid)
//                setDraggable(granted)
                }
            }
        }

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
            publicTab.setCustomView(R.layout.fcr_cloud_disk_tab_item_layout)
            (publicTab.customView?.findViewById(R.id.title) as? AppCompatTextView)?.text = tabs[0]
            binding.tabLayout.addTab(publicTab)
            publicTab.customView?.let {
                tabBindings.add(FcrCloudDiskTabItemLayoutBinding.bind(it))
            }
            val mineTab = binding.tabLayout.newTab()
            mineTab.setCustomView(R.layout.fcr_cloud_disk_tab_item_layout)
            (mineTab.customView?.findViewById(R.id.title) as? AppCompatTextView)?.text = tabs[1]
            binding.tabLayout.addTab(mineTab)
            mineTab.customView?.let {
                tabBindings.add(FcrCloudDiskTabItemLayoutBinding.bind(it))
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
                    fragmentTransAction?.replace(binding.fragmentContainer.id, fragment, tag)
                    fragmentTransAction?.commitNow()
                }

                private fun updateTabTextStyle(binding: FcrCloudDiskTabItemLayoutBinding, selected: Boolean) {
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
        }

        fun dispose() {
            fragments.clear()
            ContextCompat.getMainExecutor(binding.root.context).execute {
                container.removeView(binding.root)
            }
        }

        override fun onLoad(courseware: AgoraEduCourseware) {
            val packet = AgoraBoardInteractionPacket(LoadCourseware, courseware)
            GsonUtil.toJson(packet)?.let {
                sendMessage(it)
            }
        }
    }
}