package io.agora.online.component.teachaids.networkdisk.mycloud

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.online.component.teachaids.networkdisk.FCRCloudDiskResourceFragment
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.core.internal.log.LogX

/**
 * author : cjw
 * date : 2022/3/16
 * description :
 */
internal class FCRCloudDiskMyCloudFragment : FCRCloudDiskResourceFragment() {
    override var logTag = tagStr

    companion object {
        const val tagStr = "CloudDiskMyCloudFragmen"

        fun create(config: Pair<String, Any>?): Fragment {
            val fragment = FCRCloudDiskMyCloudFragment()
            val bundle = Bundle()
            bundle.putSerializable(data, config)
            fragment.arguments = bundle
            return fragment
        }
    }

    private var myCloudManager: MyCloudManager? = null

    // appId and userUuid
    private var config: Pair<String, String>? = null
    private var total: Int = Int.MAX_VALUE
    private var nextPageNo = 1
    private val loadMoreInterval = 10
    private val coursewareList = mutableSetOf<AgoraEduCourseware>()
    private var searchMode = false
    private var searchTotal: Int = Int.MAX_VALUE
    private var searchNextPageNo = 1
    private var searchKeyword: String = ""
    private var searchCoursewareList = mutableSetOf<AgoraEduCourseware>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        config = arguments?.getSerializable(data) as? Pair<String, String>
        config?.let {
            myCloudManager = MyCloudManager(it.first, it.second)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val lastIndex = coursewaresAdapter.currentList.size - 1
                (binding.recyclerView.layoutManager as? LinearLayoutManager)?.let {
                    val curLastVisibleIndex = it.findLastVisibleItemPosition()
                    val needLoadMore = lastIndex - curLastVisibleIndex <= loadMoreInterval
                    if (needLoadMore && !searchMode && coursewaresAdapter.currentList.size < total) {
                        LogX.i(tag, "加载更多数据：${nextPageNo + 1}")
                        fetchLoadCourseware(pageNo = ++nextPageNo)
                    } else if (needLoadMore && searchMode && coursewaresAdapter.currentList.size < searchTotal) {
                        LogX.i(tag, "加载更多搜索数据：${searchNextPageNo + 1}")
                        fetchLoadCourseware(resourceName = searchKeyword, pageNo = ++searchNextPageNo)
                    }
                }
            }
        })
        if (nextPageNo == 1 && coursewareList.size == 0) {
            // first fetch courseware list
            fetchLoadCourseware(pageNo = nextPageNo)
        }
    }

    override fun getInitCoursewareList(): List<AgoraEduCourseware> {
        return coursewareList.toList()
    }

    private fun fetchLoadCourseware(resourceName: String? = null, pageNo: Int) {
        myCloudManager?.fetchCoursewareWithPage(resourceName = resourceName, page = pageNo,
            callback = object : EduContextCallback<MyCloudCoursewareRes> {
                override fun onSuccess(target: MyCloudCoursewareRes?) {
                    target?.let { wareRes ->
                        if (!searchMode) {
                            nextPageNo = wareRes.pageNo + 1
                            total = wareRes.total
                        } else {
                            searchNextPageNo = wareRes.pageNo + 1
                            searchTotal = wareRes.total
                        }
                        if (wareRes.list != null) {
                            wareRes.list.map { CoursewareUtil.transfer(it) }.let {
                                val tmp = if (!searchMode) {
                                    coursewareList.addAll(it)
                                    coursewareList
                                } else {
                                    searchCoursewareList.addAll(it)
                                    searchCoursewareList
                                }
                                coursewaresAdapter.submitList(tmp.toList())
                            }
                        }
                    }
                }

                override fun onFailure(error: EduContextError?) {
                    Log.e(tagStr, "获取可见列表失败:${error?.let { GsonUtil.toJson(it) }}")
                }
            })
    }

    override fun onRefreshClick() {
        if (!searchMode) {
            coursewareList.clear()
            nextPageNo = 1
            fetchLoadCourseware(pageNo = nextPageNo)
        } else {
            searchCoursewareList.clear()
            searchNextPageNo = 1
            fetchLoadCourseware(resourceName = searchKeyword, pageNo = searchNextPageNo)
        }
    }

    override fun onClearClick() {
        super.onClearClick()
        searchMode = false
        searchKeyword = ""
        coursewaresAdapter.submitList(coursewareList.toList())
    }

    override fun searchWithKeyword(keyword: String) {
        searchMode = true
        searchKeyword = keyword
        searchCoursewareList.clear()
        fetchLoadCourseware(resourceName = keyword, pageNo = searchNextPageNo)
    }
}
