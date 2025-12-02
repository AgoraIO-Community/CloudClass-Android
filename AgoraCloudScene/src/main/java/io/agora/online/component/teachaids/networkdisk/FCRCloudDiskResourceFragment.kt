package io.agora.online.component.teachaids.networkdisk

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
import io.agora.online.easeim.utils.CommonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineCloudDiskResourceFragmentLayoutBinding

/**
 * author : cjw
 * date : 2022/3/16
 * description :
 */
abstract class FCRCloudDiskResourceFragment :
    Fragment(R.layout.fcr_online_cloud_disk_resource_fragment_layout),
    TextView.OnEditorActionListener, View.OnClickListener, TextWatcher {
    open var logTag = "FCRCloudDiskResourceFragment"

    companion object {
        const val data = "data"
    }

    lateinit var binding: FcrOnlineCloudDiskResourceFragmentLayoutBinding
    var coursewareLoadListener: FCRCloudCoursewareLoadListener? = null
    private var itemClickListener = object : FCRCloudItemClickListener {
        override fun onClick(courseware: AgoraEduCourseware) {
            coursewareLoadListener?.onLoad(courseware)
        }
    }
    protected val coursewaresAdapter = CoursewareListAdapter(itemClickListener)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FcrOnlineCloudDiskResourceFragmentLayoutBinding.bind(view)
        binding.refreshImg.setOnClickListener(this)
        binding.searchEd.setOnEditorActionListener(this)
        binding.searchEd.addTextChangedListener(this)
        binding.clearImg.setOnClickListener(this)
        context?.let { context ->
            binding.recyclerView.addItemDecoration(DividerItemDecoration(context, VERTICAL).apply {
                ContextCompat.getDrawable(context, R.drawable.fcr_cloud_disk_list_item_divider)
                    ?.let {
                        setDrawable(it)
                    }
            })
        }
        binding.recyclerView.adapter = coursewaresAdapter
        val tmp = getInitCoursewareList()
        coursewaresAdapter.submitList(tmp)
    }

    /**
     * 获取初始化的列表集合
     * get initial courseware list
     */
    open fun getInitCoursewareList(): List<AgoraEduCourseware> {
        return listOf()
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            CommonUtil.hideSoftKeyboard(binding.searchEd)
            if (v?.text?.isNotEmpty() == true) {
                searchWithKeyword(v.text.toString())
            }
            return true
        }
        return false
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.refreshImg.id -> {
                onRefreshClick()
            }
            binding.clearImg.id -> {
                onClearClick()
            }
        }
    }

    protected abstract fun onRefreshClick()

    protected open fun onClearClick() {
        binding.searchEd.setText("")
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable?) {
        binding.clearImg.visibility = if (binding.searchEd.text?.isNotEmpty() == true) VISIBLE else GONE
    }

    protected abstract fun searchWithKeyword(keyword: String)
}