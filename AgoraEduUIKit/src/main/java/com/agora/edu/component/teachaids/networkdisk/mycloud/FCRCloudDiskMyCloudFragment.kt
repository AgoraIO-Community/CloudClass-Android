package com.agora.edu.component.teachaids.networkdisk.mycloud

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agora.edu.component.teachaids.RightToLeftDialog
import com.agora.edu.component.teachaids.networkdisk.FCRCloudDiskResourceFragment
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.Conversion
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudDelFileReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudPresignedUrlsReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.req.MyCloudUserAndResourceReq
import com.agora.edu.component.teachaids.networkdisk.mycloud.res.MyCloudCoursewareRes
import com.agora.edu.component.teachaids.networkdisk.mycloud.upload.UploadCallback
import com.agora.edu.component.teachaids.networkdisk.mycloud.upload.UploadTask
import com.hyphenate.easeim.modules.utils.CommonUtil
import com.permissionx.guolindev.PermissionX
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.internal.base.ToastManager
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.courseware.AgoraEduCourseware
import io.agora.agoraeducore.core.internal.launch.courseware.CoursewareUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.component.dialog.AgoraUIDialogBuilder
import io.agora.util.FileHelper
import io.agora.util.VersionUtils
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * author : cjw
 * date : 2022/3/16
 * description :
 */
class FCRCloudDiskMyCloudFragment : FCRCloudDiskResourceFragment() {
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


    private lateinit var receiver:BroadcastReceiver

    private val selectFileResultCode = 9998 //获取文件reqeustCode
    private val selectImageResultCode = 9999 //获取图片requestCode

    private val PPT = "application/vnd.ms-powerpoint"
    private val PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    private val DOC = "application/msword"
    private val DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private val PDF = "application/pdf"
    private val MP4 = "video/mp4"
    private val GIF = "image/gif"
    private val MP3 = "audio/mpeg"
    private val PNG = "image/png"
    private val JPG = "image/jpeg"
    private val XLS = "application/vnd.ms-excel"
    private val XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val TXT = "text/plain"

    var userUuid:String?=null

    var resourceUuid:String? =null
    var resourceName:String? =null
    var ext:String=""
    var url:String?=null
    var fileSize:Long =0L
    var conversion:Conversion? = null

    var mCourseware: AgoraEduCourseware?=null

    var delPosition:Int=0

    var moreReqest=false
    var isUploadImage=false

    var myClouldItemClickListener= object :MyCloudItemClickListener{
        override fun onSelectClick(courseware: AgoraEduCourseware, position: Int) {
            if(position!=-1){
                delPosition=position
                mCourseware=courseware
                binding.myClouldBottomeLayout.isVisible=false
                binding.myClouldDeleteLayout.isVisible=true
            }else{
                binding.myClouldBottomeLayout.isVisible=true
                binding.myClouldDeleteLayout.isVisible=false
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        config = arguments?.getSerializable(data) as? Pair<String, String>
        config?.let {
            userUuid=it.second
            myCloudManager = MyCloudManager(it.first, it.second)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initReceiver()
        coursewaresAdapter.addMyCloudItemClickListener(myClouldItemClickListener)
        coursewaresAdapter.setMyCloudType()
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

        binding.clouldBottomLayout.visibility = View.VISIBLE

        binding.myClouldUploadFileLayout.setOnClickListener {
            PermissionX.init(context as FragmentActivity)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        selectFileFromLocal()
                    } else {
                        ToastManager.showShort(context,R.string.fcr_savecanvas_tips_save_failed_tips)
                        LogX.e(tagStr, "没有权限")
                    }
                }
        }

        binding.myClouldUploadImgLayout.setOnClickListener {
            PermissionX.init(context as FragmentActivity)
                .permissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .request { allGranted, grantedList, deniedList ->
                    if (allGranted) {
                        selectPicFromLocal()
                    } else {
                        ToastManager.showShort(context,R.string.fcr_savecanvas_tips_save_failed_tips)
                        LogX.e(tagStr, "没有权限")
                    }
                }
        }

        binding.myClouldDeleteLayout.setOnClickListener {
            context?.let {
                context?.resources?.let { it1 ->
                    AgoraUIDialogBuilder(it)
                        .title(it1.getString(R.string.fcr_my_cloud_title))
                        .message(it1.getString(R.string.fcr_my_cloud_content))
                        .negativeText(it1.getString(R.string.fcr_my_cloud_cancel))
                        .positiveText(it1.getString(R.string.fcr_my_cloud_submit))
                        .positiveTextColor(it1.getColor(R.color.fcr_text_commit_color))
                        .positiveClick {
                            delFileReqest()
                        }
                        .build()
                        .show()
                }
            }
        }

        binding.mycloudHelpImg.setOnClickListener {
            context?.let { it1 -> RightToLeftDialog(it1).show() }
        }
    }

    override fun getInitCoursewareList(): List<AgoraEduCourseware> {
        return coursewareList.toList()
    }

    var handler = Handler()
    var requestPageNoMap = ConcurrentHashMap<Int, Boolean>()

    private fun fetchLoadCourseware(resourceName: String? = null, pageNo: Int) {
        if (requestPageNoMap[pageNo] == true) {
            LogX.e(logTag, "fetchLoadCourseware already request : $pageNo")
            return
        }
        LogX.e(logTag, "fetchLoadCourseware new request : $pageNo")
        requestPageNoMap[pageNo] = true
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


                                run breaking@{
                                    tmp.forEach continuing@{ it1 ->
                                        if(it1.taskProgress?.status == "Converting"){
                                            handler.removeCallbacksAndMessages(null)
                                            handler.postDelayed({
                                                onRefreshClick()
                                            },5000)
                                            return@breaking
                                        }
                                    }
                                }
                                coursewaresAdapter.submitList(tmp.toList(), Runnable {
                                    coursewaresAdapter.notifyDataSetChanged()
                                    if(pageNo==1 && coursewareList.size>0){
                                        binding.recyclerView.layoutManager?.scrollToPosition(0)
                                    }
                                })
                            }
                        }
                    }

                    requestPageNoMap.remove(pageNo)
                }

                override fun onFailure(error: EduContextError?) {
                    requestPageNoMap.remove(pageNo)
                    Log.e(logTag, "获取可见列表失败:${error?.let { GsonUtil.toJson(it) }}")
                }
            })
    }

    public override fun onRefreshClick() {
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

    /**
     * 选择本地相册
     */
    private fun selectPicFromLocal() {
        val intent: Intent?
        if (VersionUtils.isTargetQ(context)) {
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
        } else {
            intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        intent.type = "image/*"
        val activity = context as Activity
        activity.startActivityForResult(intent, selectImageResultCode)
    }

    /**
     * 选择本地文件
     */
    private fun selectFileFromLocal() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "$PPT|$PPTX|$DOC|$DOCX|$PDF|$MP4|$GIF|$MP3|$PNG|$JPG|$XLS|$XLSX|$TXT"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(PPT,PPTX,DOC,DOCX,PDF,MP4,GIF,MP3,PNG,JPG,XLS,XLSX,TXT))
        val activity = context as Activity
        activity.startActivityForResult(intent, selectFileResultCode)
    }

    private fun initReceiver() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(con: Context?, intent: Intent?) {
                val data = intent?.getParcelableExtra<Uri>(
                    context?.resources
                        ?.getString(R.string.my_clould_select_image_key)
                )
                when(intent?.action){
                    context?.packageName.plus(
                        context?.resources?.getString(R.string.my_clould_select_file_action)) -> {
                        data?.let {
                            val filePath = FileHelper.getInstance().getFilePath(data)
                            if (filePath.isNotEmpty() && File(filePath).exists()) {
                                sendFileMessage(Uri.parse(filePath))
                            } else {
                                CommonUtil.takePersistableUriPermission(context, it)
                                sendFileMessage(it)
                            }
                        }
                    }
                    context?.packageName.plus(
                        context?.resources?.getString(R.string.my_clould_select_image_action)) -> {
                        data?.let {
                            val filePath = FileHelper.getInstance().getFilePath(data)
                            if (filePath.isNotEmpty() && File(filePath).exists()) {
                                sendImageMessage(Uri.parse(filePath))
                            } else {
                                CommonUtil.takePersistableUriPermission(context, it)
                                sendImageMessage(it)
                            }
                        }
                    }
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(context?.packageName.plus(
            context?.resources?.getString(R.string.my_clould_select_file_action)))
        intentFilter.addAction(context?.packageName.plus(
            context?.resources?.getString(R.string.my_clould_select_image_action)))
        context?.registerReceiver(receiver, intentFilter)
    }

    fun sendImageMessage(uri: Uri){
        if(moreReqest){
            return
        }
        isUploadImage=true
        requestParams(uri)
    }

    fun sendFileMessage(uri: Uri){
        if(moreReqest){
            return
        }
        isUploadImage=false
        requestParams(uri)
    }


    private fun requestParams(uri: Uri){
        ext =getExt(FileHelper.getInstance().getFileMimeType(uri))
        resourceName=FileHelper.getInstance().getFilename(uri)
        fileSize = FileHelper.getInstance().getFileLength(uri)

        var tempType = FileHelper.getInstance().getFileMimeType(uri)
        conversion=null
        if(PDF == tempType || PPT == tempType || PPTX == tempType || DOC == tempType || DOCX ==tempType){
            conversion = Conversion().apply {
                if(PPTX == tempType) {
                    type="dynamic"
                }else{
                    type="static"
                }
            }
        }


        context?.let {
            var relealFilePath= FileHelper.getInstance().getFilePath(uri)
            if(relealFilePath ==null || relealFilePath == ""){
                relealFilePath = MyCloudUriUtils.getFileAbsolutePath(context,uri)
            }
            if(relealFilePath ==null || relealFilePath == ""){
                relealFilePath= FileUtils.getPath(it,uri)
            }
            if(relealFilePath ==null || relealFilePath == ""){
                ToastManager.showShort(it.resources.getString(R.string.fcr_my_cloud_select_image_fail))
            }else{
                presignedUrls(FileHelper.getInstance().getFilename(uri),FileHelper.getInstance().getFileMimeType(uri),
                    relealFilePath
                )
            }
        }
    }


    private fun getExt(mimeType: String):String{
        var res=""
        when(mimeType){
            PPT -> res="ppt"
            PPTX -> res="pptx"
            DOC -> res="doc"
            DOCX -> res="docx"
            PDF -> res="pdf"
            MP4 -> res="mp4"
            GIF -> res="gif"
            MP3 -> res="mp3"
            GIF -> res="gif"
            PNG -> res="png"
            JPG -> res="jpeg"
            XLS -> res="xls"
            XLSX -> res="xlsx"
            TXT -> res="txt"
        }
        return res
    }

    fun laodingAnimation():Animation{
        var rotate =  RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        rotate.interpolator = LinearInterpolator()
        rotate.duration=500//设置动画持续周期
        rotate.repeatCount =Animation.INFINITE
        rotate.fillAfter=true//动画执行完后是否停留在执行完的状态
        return rotate
    }

    fun resetUploadBtn(){
        binding.myClouldUploadFileLayout.isClickable=true
        binding.myClouldUploadImgLayout.isClickable=true
        binding.progressImgBarLayout.isVisible=false
        binding.progressImgBar.animation?.let { it1 ->
            it1.cancel()
        }

        binding.progressFileBarLayout.isVisible=false
        binding.progressFileBar.animation?.let { it1 ->
            it1.cancel()
        }
    }

    private fun presignedUrls(fileName:String,fileType:String,filePath:String) {

        if(isUploadImage){
            binding.progressImgBarLayout.isVisible=true
            binding.progressImgBar.animation =laodingAnimation()
        }else{
            binding.progressFileBarLayout.isVisible=true
            binding.progressFileBar.animation =laodingAnimation()
        }

        moreReqest=true
        binding.myClouldUploadFileLayout.isClickable=false
        binding.myClouldUploadImgLayout.isClickable=false
        myCloudManager?.presignedUrls(params = arrayListOf(MyCloudPresignedUrlsReq(fileName,fileType,ext,fileSize,conversion)),
            callback = object : EduContextCallback<List<MyCloudPresignedUrlsRes>> {
                override fun onSuccess(result: List<MyCloudPresignedUrlsRes>?) {
                    moreReqest=false
                    result?.get(0)?.resourceUuid?.let {
                        resourceUuid=it
                    }

                    result?.get(0)?.url?.let {
                        url=it
                    }

                    result?.get(0)?.preSignedUrl?.let {
                        uploadFile(it,filePath,fileType)
                    }

                }

                override fun onFailure(error: EduContextError?) {
                    moreReqest=false
                    resetUploadBtn()
                    context?.let {
                        ToastManager.showShort(it.resources.getString(R.string.fcr_my_cloud_upload_fail))
                    }
                    Log.e(tagStr, "生成预签名失败:${error?.let { GsonUtil.toJson(it) }}")
                }
            })
    }

    private fun uploadFile(url: String,filePath: String,mimeType: String){
        val uploadTask =
            UploadTask(url, filePath,mimeType, object : UploadCallback {
                override fun onProgressUpdate(path: String?, percentage: Int) {
                    LogX.d("进度"+(1 - percentage / 100f))
                }

                override fun onError(code: Int, error: String?) {
                    resetUploadBtn()
                    context?.let {
                        ToastManager.showShort(it.resources.getString(R.string.fcr_my_cloud_upload_fail))
                    }
                    Log.e(tagStr, "上传图片失败:${error?.let { GsonUtil.toJson(it) }}")
                }

                override fun onSuccess(code: Int) {
                    buildUserAndResource()
                }
            })

        uploadTask.start()
    }

    private fun buildUserAndResource(){
        if (resourceUuid != null) {
            resourceName?.let {
                ext?.let { it1 ->
                    url?.let { it2 ->
                        MyCloudUserAndResourceReq(
                            resourceName = it,size = fileSize,ext = it1,url = it2,
                            conversion
                        ).also {
                            myCloudManager?.buildUserAndResource(resourceUuid = resourceUuid!!,
                                params = it,
                                callback = object : EduContextCallback<Any> {
                                    override fun onSuccess(result: Any?) {
                                        onRefreshClick()
                                        resetUploadBtn()
                                    }

                                    override fun onFailure(error: EduContextError?) {
                                        resetUploadBtn()
                                        if(error?.code!=30409450) {
                                            context?.let {
                                                ToastManager.showShort(it.resources.getString(R.string.fcr_my_cloud_upload_fail))
                                            }
                                        }
                                        Log.e(tagStr, "添加用户资源失败:${error?.let { GsonUtil.toJson(it) }}")
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    private fun delFileReqest() {
        var myCloudDelFileReq= mCourseware?.resourceUuid?.let { userUuid?.let { it1 ->
            MyCloudDelFileReq(it,
                it1
            )
        } }
        myCloudDelFileReq?.let {
            myCloudManager?.delFileRequest(
                arrayListOf(myCloudDelFileReq),
                callback = object : EduContextCallback<Any> {
                    override fun onSuccess(result: Any?) {
                        coursewareList.remove(mCourseware)
                        coursewaresAdapter.submitList(coursewareList.toList(), Runnable {
                            coursewaresAdapter.resetCurrentPosition()
                        })
                        binding.myClouldDeleteLayout.isVisible = false
                        binding.myClouldBottomeLayout.isVisible =true
                    }

                    override fun onFailure(error: EduContextError?) {
                        context?.let {
                            ToastManager.showShort(it.resources.getString(R.string.fcr_my_cloud_delete_fail))
                        }
                        Log.e(tagStr, "删除文件失败:${error?.let { GsonUtil.toJson(it) }}")
                    }
                })
        }

    }

    fun registerReceiver(){
        initReceiver()
    }

    fun unregisterReceiver(){
        try {
            context?.unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            context?.unregisterReceiver(receiver)
        } catch (e: Exception) {
        }
    }
}
