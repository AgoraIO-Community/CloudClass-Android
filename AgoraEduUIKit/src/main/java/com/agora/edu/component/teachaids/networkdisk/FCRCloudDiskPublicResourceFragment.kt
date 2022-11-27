package com.agora.edu.component.teachaids.networkdisk

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware

/**
 * author : cjw
 * date : 2022/3/16
 * description :
 */
internal class FCRCloudDiskPublicResourceFragment : FCRCloudDiskResourceFragment() {
    override var logTag = tagStr
    companion object {
        const val tagStr = "FCRCloudDiskPublicResourceFragment"

        fun create(pubResourceList: ArrayList<AgoraEduCourseware>): Fragment {
            val fragment = FCRCloudDiskPublicResourceFragment()
            val bundle = Bundle()
            bundle.putParcelableArrayList(data, pubResourceList)
            fragment.arguments = bundle
            return fragment
        }
    }
    private var publicCoursewares: ArrayList<AgoraEduCourseware>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        publicCoursewares = arguments?.getParcelableArrayList(data)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.clouldBottomLayout.visibility = View.GONE
    }

    override fun getInitCoursewareList(): List<AgoraEduCourseware> {
        return publicCoursewares ?: listOf()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.refreshImg.id -> {
            }
            binding.clearImg.id -> {
                binding.searchEd.setText("")
                coursewaresAdapter.submitList(publicCoursewares?.toMutableList())
            }
        }
    }

    override fun onRefreshClick() {
    }

    override fun onClearClick() {
        super.onClearClick()
        coursewaresAdapter.submitList(publicCoursewares?.toMutableList())
    }

    override fun searchWithKeyword(keyword: String) {
        publicCoursewares?.filter { it.resourceName?.contains(keyword) == true }
            .let {
                coursewaresAdapter.submitList(it)
            }
    }
}