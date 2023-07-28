package com.agora.edu.component.teachaids.networkdisk

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.teachaids.networkdisk.mycloud.MyCloudItemClickListener
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeduuikit.databinding.FcrCloudDiskListItemLayoutBinding
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
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.R

/**
 * 课件列表适配器
 * Courseware list adapter
 */
class CoursewareListAdapter(private val itemClickListener: FCRCloudItemClickListener?) :
    ListAdapter<AgoraEduCourseware, ViewHolder>(UserListDiff()) {

    var isMyCloudType=false
    var currentPosition = -1
    var myCloudItemClickListener:MyCloudItemClickListener?=null

    fun addMyCloudItemClickListener(myCloudItemClickListener:MyCloudItemClickListener){
        this.myCloudItemClickListener=myCloudItemClickListener
    }

    fun resetCurrentPosition(){
        currentPosition=-1;
        notifyDataSetChanged()
    }

    fun setMyCloudType(){
        isMyCloudType=true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FcrCloudDiskListItemLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.binding.root.setOnClickListener {
            if("Finished" == item.taskProgress?.status || item.taskProgress?.status ==null) {
                itemClickListener?.onClick(item)
            }
        }

        holder.binding.checkbox.setOnClickListener {
            if(currentPosition == position){
                currentPosition=-1
            }else{
                currentPosition=position
            }
            notifyDataSetChanged()
            myCloudItemClickListener?.onSelectClick(item,currentPosition)
        }

        if(position == currentPosition){
            holder.binding.checkbox.setImageResource(R.mipmap.my_clould_checkbox_selected)
        }else{
            holder.binding.checkbox.setImageResource(R.mipmap.my_clould_checkbox_unselected)
        }

        if(isMyCloudType){
            holder.binding.checkbox.visibility=View.VISIBLE
        }else{
            holder.binding.checkbox.visibility=View.GONE
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

class ViewHolder(val binding: FcrCloudDiskListItemLayoutBinding) :
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
        binding.stateText.visibility = View.GONE
        binding.checkbox.visibility = View.VISIBLE
        binding.percentLayout.visibility=View.GONE

        when(courseware.taskProgress?.status){
            "Fail" -> {
                binding.stateText.visibility = View.VISIBLE
                binding.stateText.text =courseware.taskProgress?.status
            }
            "Converting" -> {
                binding.percentLayout.visibility=View.VISIBLE
                binding.progressFileBar.animation=laodingAnimation()
                binding.percentText.text = "${courseware.taskProgress?.convertedPercentage}%"
                binding.checkbox.visibility = View.GONE

            }
            "Finished" -> ""
            "Waiting" -> ""
        }
    }

    fun laodingAnimation(): Animation {
        var rotate =  RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotate.interpolator = LinearInterpolator()
        rotate.duration=500//设置动画持续周期
        rotate.repeatCount = Animation.INFINITE
        rotate.fillAfter=true//动画执行完后是否停留在执行完的状态
        return rotate
    }
}
