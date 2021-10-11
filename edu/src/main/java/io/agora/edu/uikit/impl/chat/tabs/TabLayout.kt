package io.agora.edu.uikit.impl.chat.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.agora.edu.R
import io.agora.edu.uikit.impl.chat.AgoraUIChatItem
import kotlin.properties.Delegates

class TabManager(
        private val tabLayout: TabLayout,
        private val configs: List<ChatTabConfig>,
        private val viewPager: ViewPager2,
        eduContextPool: io.agora.edu.core.context.EduContextPool?,
        private var listener: OnTabSelectedListener? = null) {

    private val tabAdapter: ChatTabAdapter

    init {
        val title = Array(configs.size, object : ((Int) -> String) {
            override fun invoke(index: Int): String {
                return configs[index].title
            }
        })

        tabLayout.setTabs(title, object : TabListener {
            override fun onTabSelected(position: Int) {
                if (viewPager.currentItem != position) {
                    viewPager.currentItem = position
                }

                tabLayout.showRedDot(position, false)
                listener?.onTabSelected(position)
            }
        })

        tabAdapter = ChatTabAdapter(viewPager, tabLayout.context, eduContextPool, this, configs)
        viewPager.adapter = tabAdapter
        viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (tabLayout.getSelected() != position) {
                    tabLayout.setSelected(position)
                    tabAdapter.getChatTab(position)?.isActive(true)
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    tabAdapter.getAllTabs().forEach { tab ->
                        tab.isActive(false)
                    }
                }
            }
        })
    }

    fun setCurrent(position: Int) {
        if (position < 0 || position > configs.size) {
            return
        }

        tabLayout.setSelected(position)
    }

    fun getCurrentTab(): ChatTabBase? {
        return tabAdapter.getChatTab(viewPager.currentItem)
    }

    fun getTab(type: TabType): ChatTabBase? {
        return tabAdapter.getTabChat(type)
    }

    fun getTab(index: Int): ChatTabBase? {
        return tabAdapter.getChatTab(index)
    }

    fun addMessage(type: TabType, item: AgoraUIChatItem) {
        getTab(type)?.let { tab ->
            tab.addMessage(item)
            val position = tabAdapter.getPosition(tab)
            if (position >= 0 && getCurrentTab() != tab && tab.hasUnreadMessages()) {
                tabLayout.showRedDot(position, true)
            }
        }
    }

    fun addMessageList(type: TabType, list: List<AgoraUIChatItem>, front: Boolean) {
        getTab(type)?.let { tab ->
            tab.addMessageList(list, front)
            val position = tabAdapter.getPosition(tab)
            if (position >= 0 && getCurrentTab() != tab && tab.hasUnreadMessages()) {
                tabLayout.showRedDot(position, true)
            }
        }
    }

    fun allowChat(group: Boolean?, local: Boolean?) {
        tabAdapter.getAllTabs().forEach { tab ->
            tab.allowChat(group, local)
        }
    }
}

interface OnTabSelectedListener {
    fun onTabSelected(position: Int)
}

data class ChatTabConfig(
        val title: String,
        val type: TabType,
        val peerUser: io.agora.edu.core.context.EduContextUserInfo?
)

enum class TabType {
    Public, Private
}

class ChatTabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tab: ChatTabBase? = null

    init {
        val container: ViewGroup? = itemView as? ViewGroup
        val child: View? =
                if (container == null || container.childCount == 0) null
                else container.getChildAt(0)
        tab = child as? ChatTabBase
    }
}

class ChatTabAdapter(
        private val viewPager: ViewPager2,
        private val context: Context,
        private val eduContextPool: io.agora.edu.core.context.EduContextPool?,
        private val tabManager: TabManager,
        private val configs: List<ChatTabConfig>) : RecyclerView.Adapter<ChatTabViewHolder>() {

    private val tag = "ChatTabAdapter"
    private val tabList: List<ChatTabBase>

    init {
        val list = mutableListOf<ChatTabBase>()
        configs.forEach { config ->
            list.add(createTab(config, viewPager))
        }
        tabList = list.toList()
    }

    private fun createTab(config: ChatTabConfig, container: ViewGroup): ChatTabBase {
        return when (config.type) {
            TabType.Public -> PublicChatTab(context, container).apply {
                this.eduContext = eduContextPool
                this.setTabManager(tabManager)
            }
            TabType.Private -> PrivateChatTab(context, container).apply {
                this.eduContext = eduContextPool
                this.peerUser = config.peerUser
                this.setTabManager(tabManager)
            }
        }
    }

    fun getAllTabs(): List<IChatTab> {
        val list = mutableListOf<IChatTab>()
        tabList.forEach { tab ->
            list.add(tab)
        }
        return list
    }

    fun getChatTab(position: Int): ChatTabBase? {
        if (position < 0 || position >= itemCount) {
            return null
        }

        return tabList[position]
    }

    fun getTabChat(type: TabType): ChatTabBase? {
        for (i in tabList.indices) {
            if (tabList[i].getType() == type) {
                return tabList[i]
            }
        }

        return null
    }

    fun getPosition(tab: ChatTabBase): Int {
        for (i in tabList.indices) {
            if (tab == tabList[i]) {
                return i
            }
        }

        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatTabViewHolder {
        val itemView = FrameLayout(parent.context)
        itemView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        return ChatTabViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ChatTabViewHolder, position: Int) {
        val tab = getChatTab(holder.adapterPosition)
        (holder.itemView as? ViewGroup)?.addView(tab,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun getItemCount(): Int {
        return configs.size
    }
}

class TabLayout : RelativeLayout {
    private var listener: TabListener? = null
    private lateinit var tabLayout: LinearLayout
    private var selected = -1

    private var margin by Delegates.notNull<Int>()
    private var padding by Delegates.notNull<Int>()
    private var dotSize by Delegates.notNull<Int>()
    private var tabMinWidth by Delegates.notNull<Int>()

    constructor(context: Context): super(context) {
        initViews()
    }

    constructor(context: Context,  attrs: AttributeSet) : super(context, attrs) {
        initViews()
    }

    private fun initViews() {
        margin = resources.getDimensionPixelSize(R.dimen.agora_message_tab_item_margin)
        padding = resources.getDimensionPixelSize(R.dimen.agora_message_tab_item_padding)
        dotSize = resources.getDimensionPixelSize(R.dimen.agora_message_tab_dot_size)
        tabMinWidth = resources.getDimensionPixelSize(R.dimen.agora_message_tab_min_width)

        tabLayout = LinearLayout(context)
        tabLayout.orientation = LinearLayout.HORIZONTAL
        addView(tabLayout, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    fun setTabs(tabs: Array<String>, listener: TabListener?) {
        tabLayout.removeAllViews()
        this.listener = listener
        makeTabViews(tabs)
        setSelectedTab(0)
    }

    @SuppressLint("UseCompatLoadingForColorStateLists")
    private fun makeTabViews(tabs: Array<String>) {
        for (i in tabs.indices) {
            val name = tabs[i]
            val layout = LayoutInflater.from(context).inflate(
                    R.layout.agora_chat_tab_item_layout, tabLayout, false)
            layout.setOnClickListener {
                setSelected(i)
                listener?.onTabSelected(i)
            }

            val text = layout.findViewById<AppCompatTextView>(R.id.agora_chat_tab_item_text)
            text.text = name

            val dot = layout.findViewById<AppCompatImageView>(R.id.agora_chat_tab_item_dot)
            dot.visibility = View.GONE

            tabLayout.addView(layout)
            val tabParams = layout.layoutParams as MarginLayoutParams
            tabParams.leftMargin = margin
            layout.layoutParams = tabParams
        }
    }

    @Synchronized fun setSelected(position: Int) {
        var pos = position
        if (pos < 0 || pos >= tabLayout.childCount) {
            pos = 0
        }

        setSelectedTab(pos)
    }

    private fun setSelectedTab(tab: Int) {
        tabLayout.post {
            if (tab != selected) {
                listener?.onTabSelected(tab)
            }

            selected = tab
            for (i in 0 until tabLayout.childCount) {
                tabLayout.getChildAt(i).isActivated = i == tab
            }
        }
    }

    fun getSelected() : Int {
        return selected
    }

    @UiThread
    fun showRedDot(position: Int, show: Boolean) {
        if (position < 0 || position >= tabLayout.childCount) {
            return
        }

        tabLayout.post {
            val child = tabLayout.getChildAt(position)
            val dot = child.findViewById<AppCompatImageView>(R.id.agora_chat_tab_item_dot)
            dot?.let {
                it.visibility = if (show) VISIBLE else GONE
            }
        }
    }
}

interface TabListener {
    fun onTabSelected(position: Int)
}