package io.agora.online.component.teachaids.networkdisk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.mp3
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.png
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.jpg
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.jpeg
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.txt
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.unknown
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.mp4
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.ppt
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.doc
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.xls
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.pdf
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCoursewareExt.alf
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineCloudDiskListItemLayoutBinding

/**
 * 课件列表适配器
 * Courseware list adapter
 */
class CoursewareListAdapter(private val itemClickListener: FCRCloudItemClickListener?) :
    ListAdapter<AgoraEduCourseware, ViewHolder>(UserListDiff()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FcrOnlineCloudDiskListItemLayoutBinding.inflate(LayoutInflater.from(parent.context))
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            itemClickListener?.onClick(item)
        }
    }
}

internal class UserListDiff : DiffUtil.ItemCallback<AgoraEduCourseware>() {
    override fun areItemsTheSame(
        oldItem: AgoraEduCourseware,
        newItem: AgoraEduCourseware
    ): Boolean {
        return oldItem == newItem && oldItem.resourceUuid == newItem.resourceUuid
    }

    override fun areContentsTheSame(
        oldItem: AgoraEduCourseware,
        newItem: AgoraEduCourseware
    ): Boolean {
        return oldItem.resourceUuid == newItem.resourceUuid
            && oldItem.resourceName == newItem.resourceName
            && oldItem.ext == newItem.ext
            && oldItem.resourceUrl == newItem.resourceUrl
            && oldItem.scenePath == newItem.scenePath
            && oldItem.scenes == newItem.scenes
    }
}

class ViewHolder(val binding: FcrOnlineCloudDiskListItemLayoutBinding) :
    RecyclerView.ViewHolder(binding.root) {
    private val iconMap: Map<String, Int> = mutableMapOf(
        Pair(mp3.name, R.drawable.fcr_cloud_disk_audio_ic), Pair(png.name, R.drawable.fcr_cloud_disk_pic_ic),
        Pair(jpg.name, R.drawable.fcr_cloud_disk_pic_ic), Pair(jpeg.name, R.drawable.fcr_cloud_disk_pic_ic),
        Pair(txt.name, R.drawable.fcr_cloud_disk_txt_ic), Pair(unknown.name, R.drawable.fcr_cloud_disk_unknown_ic),
        Pair(mp4.name, R.drawable.fcr_cloud_disk_video_ic), Pair(ppt.name, R.drawable.fcr_cloud_disk_ppt_ic),
        Pair(doc.name, R.drawable.fcr_cloud_disk_word_ic), Pair(xls.name, R.drawable.fcr_cloud_disk_xls_ic),
        Pair(pdf.name, R.drawable.fcr_cloud_disk_pdf_ic), Pair(alf.name, R.drawable.fcr_cloud_disk_alf_ic)
    )

    fun bind(courseware: AgoraEduCourseware) {
        val icon = iconMap[courseware.ext]
        icon?.let {
            binding.icon.setImageResource(it)
        }
        binding.name.text = courseware.resourceName
    }
}
