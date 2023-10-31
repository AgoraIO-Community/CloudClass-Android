package io.agora.online.component.teachaids.vote

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.agora.online.component.teachaids.AgoraTeachAidWidgetActiveStateChangeData
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionPacket
import io.agora.online.component.teachaids.AgoraTeachAidWidgetInteractionSignal.ActiveState
import io.agora.online.component.teachaids.TeachAidStatics.EXTRA_KEY_APPID
import io.agora.online.component.teachaids.vote.VoteState.*
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessage
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetFrame
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.online.R
import io.agora.online.component.teachaids.AgoraTeachAidMovableWidget
import io.agora.online.databinding.FcrOnlineVoteWidgetContentBinding
import io.agora.online.databinding.FcrOnlineWidgetVoteBuildChoiceLayoutBinding
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * author : cjw
 * date : 2022/2/14
 * description : 投票器
 */
class AgoraTeachAidVoteWidget : AgoraTeachAidMovableWidget() {
    override val TAG = "AgoraVoteWidget"
    private var voteContent: AgoraVoteWidgetContent? = null

    override fun init(container: ViewGroup) {
        super.init(container)
        container.post {
            widgetInfo?.localUserInfo?.let {
                voteContent = AgoraVoteWidgetContent(container, it)
            }
        }
    }

    override fun isNeedRelayout(): Boolean {
        return true
    }

    fun getWidgetMsgObserver(): AgoraWidgetMessageObserver? {
        return null
    }

    override fun onSyncFrameUpdated(frame: AgoraWidgetFrame) {
        //super.onSyncFrameUpdated(frame)
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        voteContent?.parseProperties(properties, widgetInfo?.localUserProperties)
    }

    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
        voteContent?.parseProperties(properties, widgetInfo?.localUserProperties)
    }

    override fun release() {
        voteContent?.dispose()
        super.release()
    }

    internal inner class AgoraVoteWidgetContent(val container: ViewGroup, val localUserInfo: AgoraWidgetUserInfo) {
        private val tag = "AgoraVoteWidgetContent"

        private val binding = FcrOnlineVoteWidgetContentBinding.inflate(LayoutInflater.from(container.context), container, true)
        private lateinit var studentChoiceAdapter: ChoiceAdapter
        private var mStudentState = Init.value
        private var mTeacherState = Init.value
        private var voteUserData: VoteUserData? = null
        private var voteData: VoteData? = null
        private var voteManager: VoteManager? = null
        private var buildChoiceAdapter: BuildChoiceAdapter? = null

        // at last two options and at most five options
        private val minChoiceNum = 2
        private val maxChoiceNum = 5
        private val initialChoices = listOf<String?>(null, null)

        init {
            readyUI()
            widgetInfo?.let { info ->
                if (info.roomProperties?.isNotEmpty() == true) {
                    parseProperties(info.roomProperties!!, info.localUserProperties)
                }
                info.extraInfo?.let {
                    val appId = (it as? Map<*, *>)?.get(EXTRA_KEY_APPID)
                    if (appId != null && appId.toString().isNotEmpty()) {
                        voteManager = VoteManager(appId.toString(), widgetInfo!!.roomInfo.roomUuid)
                    } else {
                        LogX.e(tag, "appId is empty, please check widgetInfo.extraInfo")
                    }
                }
            }
        }

        fun dispose() {
            //container.removeView(binding.root)
        }

        @Synchronized
        fun parseProperties(roomProperties: Map<String, Any>, userProperties: Map<String, Any>?) {
            if (userProperties?.isNotEmpty() == true) {
                val voteUserJson = GsonUtil.toJson(userProperties)
                voteUserData = voteUserJson?.let { GsonUtil.jsonToObject<VoteUserData>(it) }
            }
            if (roomProperties.isNullOrEmpty()) {
                LogX.e(tag, "cur properties is empty, please check!")
                return
            }
            val voteDataJson = GsonUtil.toJson(roomProperties)
            if (voteDataJson.isNullOrEmpty()) {
                LogX.e(tag, "cur properties json is empty, please check!")
                return
            }
            voteData = GsonUtil.jsonToObject<VoteData>(voteDataJson)
            if (voteData == null) {
                LogX.e(tag, "vote transform failed, please check!")
                return
            }
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            val state = voteData!!.pollState
            if (!isTeacher) {//不是老师
                if (mStudentState != Submitted.value) {
                    mStudentState = state
                }
                if (studentIsSubmitted(voteUserData, voteData)) {
                    mStudentState = Submitted.value
                }
                if (mStudentState == Submitted.value || mStudentState == End.value) {
                    showResultForStudent(voteData!!)
                } else if (mStudentState == Polling.value) {
                    showChoicesForStudent(voteData!!)
                }
            } else {
                if (mStudentState != Submitted.value) {
                    mTeacherState = state
                }
                if (mTeacherState == Polling.value || mTeacherState == End.value) {
                    showResultForTeacher(voteData!!)
                } else {
                }
            }
        }

        private fun readyUI() {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (isTeacher) {
                binding.voteSelectionMode.visibility = GONE
                binding.closeImg.visibility = VISIBLE
                binding.closeImg.setOnClickListener { onTeacherSwitchSelf() }
                binding.voteTitleEdit.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    }

                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        val voteTitleIsNotEmpty = !binding.voteTitleEdit.text.isNullOrEmpty()
                        val voteChoiceReady = buildChoiceAdapter?.choiceIsReady() == true
                        binding.voteBtn.isEnabled = voteTitleIsNotEmpty && voteChoiceReady
                    }
                })
                binding.singleImg.isSelected = true
                val singleOnClick = {
                    binding.multiImg.isSelected = false
                    binding.singleImg.isSelected = true
                }
                binding.singleText.setOnClickListener { singleOnClick() }
                binding.singleImg.setOnClickListener { singleOnClick() }
                val multiOnClick = {
                    binding.multiImg.isSelected = true
                    binding.singleImg.isSelected = false
                }
                binding.multiText.setOnClickListener { multiOnClick() }
                binding.multiImg.setOnClickListener { multiOnClick() }
                buildChoiceAdapter = BuildChoiceAdapter(initialChoices.toMutableList())
                buildChoiceAdapter?.voteChoiceReadyListener = object : BuildChoiceAdapter.VoteChoiceReadyListener {
                    override fun onReady(ready: Boolean) {
                        val voteTitleIsNotEmpty = !binding.voteTitleEdit.text.isNullOrEmpty()
                        binding.voteBtn.isEnabled = ready && voteTitleIsNotEmpty
                    }
                }
                binding.voteChoices.adapter = buildChoiceAdapter
                binding.incrementImg.setOnClickListener {
                    buildChoiceAdapter?.let {
                        it.refreshData()
                        binding.incrementImg.visibility = if (it.choiceList.size == maxChoiceNum) GONE else VISIBLE
                        binding.decrementImg.visibility = VISIBLE
                    }
                }
                binding.voteBtn.setOnClickListener { onTeacherStart() }
                binding.decrementImg.setOnClickListener {
                    buildChoiceAdapter?.let {
                        it.refreshData(add = false)
                        binding.incrementImg.visibility = VISIBLE
                        binding.decrementImg.visibility = if (it.choiceList.size == minChoiceNum) GONE else VISIBLE
                    }
                }
            } else {
                binding.closeImg.visibility = GONE
                binding.voteSelectionMode.visibility = VISIBLE
                binding.voteTitleEdit.visibility = GONE
                binding.voteSelectionModeLayout.visibility = GONE
                binding.voteBtn.setOnClickListener { onStudentSubmit() }
                studentChoiceAdapter = ChoiceAdapter(listOf(), false)
                binding.voteChoices.adapter = studentChoiceAdapter
                studentChoiceAdapter.setOnCheckedTextView(object : ChoiceAdapter.OnCheckChangedListener {
                    override fun onChanged(isChecked: Boolean) {
                        binding.voteBtn.isEnabled = isChecked
                    }
                })
            }

            binding.fcrVoteHidden.setOnClickListener {
                // hidden
                val message = AgoraWidgetMessage()
                message.action = 1
                onReceiveMessageForWidget?.invoke(message)
            }
        }

        private fun onStudentSubmit() {
            voteData?.let {
                widgetInfo?.localUserInfo?.let { info ->
                    val selectIndex = studentChoiceAdapter.getCheckedItems()
                    if (selectIndex.isNotEmpty()) {
                        voteManager?.submit(it.pollId, info.userUuid, selectIndex)
                    } else {
                        LogX.w(tag, "selectIndex is empty, please select item.")
                    }
                }
            }
            mStudentState = Submitted.value
            voteData?.let {
                showResultForStudent(it)
            }
        }

        private fun studentIsSubmitted(voteUserData: VoteUserData?, voteData: VoteData?): Boolean {
            val pollingId = voteData?.pollId
            if (!pollingId.isNullOrEmpty() && voteUserData != null) {
                if (!voteUserData.pollId.isNullOrEmpty() && voteUserData.pollId == pollingId
                    && !voteUserData.selectIndex.isNullOrEmpty()
                ) {
                    return true
                }
            }
            return false
        }

        private fun showChoicesForStudent(voteData: VoteData) {
            ContextCompat.getMainExecutor(binding.root.context).execute {
                binding.voteTitleEdit.visibility = GONE
                binding.voteSelectionModeLayout.visibility = GONE
                binding.voteTitleText.visibility = VISIBLE
                if (voteData.isMultiMode()) {
                    binding.voteSelectionMode.text = binding.root.resources.getString(R.string.fcr_poll_multi)
                } else {
                    binding.voteSelectionMode.text = binding.root.resources.getString(R.string.fcr_poll_single)
                }
                binding.voteTitleText.text = voteData.pollTitle
                studentChoiceAdapter.refreshData(voteData.pollItems ?: listOf(), voteData.isMultiMode())
                binding.incrementImg.visibility = GONE
                binding.decrementImg.visibility = GONE
                if (localUserInfo.userRole == AgoraEduContextUserRole.Observer.value) {
                    binding.voteBtn.visibility = GONE
                }
            }
        }

        private fun showResultForStudent(voteData: VoteData) {
            ContextCompat.getMainExecutor(binding.root.context).execute {
                if (voteData.isMultiMode()) {
                    binding.voteSelectionMode.text = binding.root.resources.getString(R.string.fcr_poll_multi)
                } else {
                    binding.voteSelectionMode.text = binding.root.resources.getString(R.string.fcr_poll_single)
                }
                binding.voteTitleText.visibility = VISIBLE
                binding.voteTitleText.text = voteData.pollTitle
                binding.voteBtn.visibility = GONE
                binding.voteChoices.adapter = ResultAdapter(voteData)
                binding.incrementImg.visibility = GONE
                binding.decrementImg.visibility = GONE
            }
        }

        private fun onTeacherSwitchSelf(close: Boolean = true, extraProperties: Map<String, Any>? = null) {
            val packet = AgoraTeachAidWidgetInteractionPacket(
                ActiveState,
                AgoraTeachAidWidgetActiveStateChangeData(!close, extraProperties)
            )
            val json = GsonUtil.toJson(packet)
            json?.let { sendMessage(it) }
        }

        private fun onTeacherStart() {
            val mode = if (binding.singleImg.isSelected) VoteMode.Single.value else VoteMode.Multi.value
            buildChoiceAdapter?.let {
                voteManager?.startVote(mode, binding.voteTitleEdit.text.toString(), it.getAllChoiceReadied())
            }
            mTeacherState = Polling.value
        }

        private fun showResultForTeacher(voteData: VoteData) {
            ContextCompat.getMainExecutor(binding.root.context).execute {
                binding.voteSelectionMode.visibility = GONE
                binding.voteTitleEdit.visibility = GONE
                binding.voteSelectionModeLayout.visibility = GONE
                binding.voteTitleText.visibility = VISIBLE
                binding.voteTitleText.text = voteData.pollTitle
                binding.voteBtn.visibility = GONE
                binding.voteChoices.adapter = ResultAdapter(voteData)
                binding.incrementImg.visibility = GONE
                binding.decrementImg.visibility = GONE
            }
        }
    }
}


internal class ChoiceAdapter(private var pollingItems: List<String>, private var multi: Boolean) :
    RecyclerView.Adapter<ChoiceAdapter.ViewHolder>() {
    private val checkedArrays = mutableSetOf<Int>()
    private var onCheckChangedListener: OnCheckChangedListener? = null

    interface OnCheckChangedListener {
        fun onChanged(isChecked: Boolean)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.vote_choice_item_text)
        val checkView: ImageView = view.findViewById(R.id.vote_choice_item_check)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fcr_online_vote_choice_item, viewGroup, false)

        val viewHolder = ViewHolder(view)
        if (multi) {
            viewHolder.checkView.setBackgroundResource(R.drawable.agora_edu_check_bg)
        }
        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val curItem = pollingItems[position]
        viewHolder.textView.text = curItem
        viewHolder.checkView.isSelected = isCheckedPosition(position)
        viewHolder.textView.isSelected = isCheckedPosition(position)
        viewHolder.itemView.setOnClickListener {
            val contain = isCheckedPosition(position)
            viewHolder.textView.isSelected = !contain
            viewHolder.checkView.isSelected = !contain
            if (isChecked() && !multi) {
                val index = checkedArrays.elementAt(0)
                if (index > -1) {
                    notifyItemChanged(index)
                }
                checkedArrays.clear()
            }

            if (contain) {
                checkedArrays.remove(position)
            } else {
                checkedArrays.add(position)
            }
            onCheckChangedListener?.onChanged(isChecked())
        }
    }

    override fun getItemCount() = pollingItems.size

    private fun isCheckedPosition(position: Int): Boolean {
        if (checkedArrays.isEmpty()) {
            return false
        }
        return checkedArrays.indexOf(position) != -1
    }

    private fun isChecked() = checkedArrays.isNotEmpty()

    fun getCheckedItems(): List<Int> {
        return checkedArrays.toList()
    }

    fun setOnCheckedTextView(listener: OnCheckChangedListener) {
        onCheckChangedListener = listener
    }

    fun refreshData(data: List<String>, multi: Boolean) {
        this.multi = multi
        this.pollingItems = data
        notifyDataSetChanged()
    }
}

// list pollDetail for teacher or student
internal class ResultAdapter(private val voteData: VoteData) :
    RecyclerView.Adapter<ResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val choiceView: TextView = view.findViewById(R.id.vote_result_item_choice)
        val proportionView: TextView = view.findViewById(R.id.vote_result_item_proportion)
        val progressBarView: ProgressBar = view.findViewById(R.id.vote_result_item_progressBar)

        init {
            progressBarView.max = 100
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.fcr_online_vote_result_item, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val text = String.format(
            viewHolder.itemView.resources.getString(R.string.fcr_votetitle), position + 1,
            voteData.pollItems?.get(position) ?: ""
        )
        viewHolder.choiceView.text = text
        val detail = voteData.pollDetails[position.toString()]
        detail?.let {
            val percentage = (it.percentage * 100).toInt()
            val proportion = "(${it.num}) ${percentage}%"
            viewHolder.proportionView.text = proportion
            viewHolder.progressBarView.progress = percentage
        }
    }

    override fun getItemCount() = voteData.pollItems?.size ?: 0
}

// used by teacher to create vote.
internal class BuildChoiceAdapter(val choiceList: MutableList<String?>) :
    RecyclerView.Adapter<BuildChoiceAdapter.ViewHolder>() {
    class ViewHolder(val binding: FcrOnlineWidgetVoteBuildChoiceLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    interface VoteChoiceReadyListener {
        fun onReady(ready: Boolean)
    }

    var voteChoiceReadyListener: VoteChoiceReadyListener? = null
    private var choiceHint: String? = null
    private val textWatcherMap = mutableMapOf<Int, TextWatcher>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FcrOnlineWidgetVoteBuildChoiceLayoutBinding.inflate(
            LayoutInflater.from(parent.context), parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, holderPositon: Int) {
        val position = holder.absoluteAdapterPosition
        holder.binding.index.text = String.format(holder.binding.root.resources.getString(R.string.fcr_votechoiceid), position + 1)
        if (textWatcherMap[position] == null) {
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    holder.binding.choiceContent.text?.let {
                        choiceList[position] = it.toString()
                    }
                    refreshReadyStatus()
                }
            }
            textWatcherMap[position] = textWatcher
        }
        if (choiceHint?.isEmpty() == true) {
            choiceHint = holder.binding.root.resources.getString(R.string.fcr_poll_input_placeholder)
        }
        val choiceContent = choiceList[position]
        if (choiceContent?.isEmpty() == true) {
            holder.binding.choiceContent.hint = choiceHint
        } else {
            textWatcherMap.forEach {
                holder.binding.choiceContent.removeTextChangedListener(it.value)
            }
            holder.binding.choiceContent.setText(choiceContent)
        }
        holder.binding.choiceContent.addTextChangedListener(textWatcherMap[position])
    }

    private fun refreshReadyStatus() {
        choiceList.forEach {
            if (it.isNullOrEmpty()) {
                voteChoiceReadyListener?.onReady(false)
                return
            }
        }
        voteChoiceReadyListener?.onReady(true)
    }

    override fun getItemCount(): Int {
        return choiceList.size
    }

    fun refreshData(data: String? = null, add: Boolean = true) {
        if (add) {
            this.choiceList.add(data)
        } else {
            this.choiceList.removeAt(choiceList.size - 1)
        }
        notifyDataSetChanged()
        refreshReadyStatus()
    }

    fun choiceIsReady(): Boolean {
        var ready = true
        choiceList.forEach {
            if (it.isNullOrEmpty()) {
                ready = false
                return@forEach
            }
        }
        return ready
    }

    fun getAllChoiceReadied(): MutableList<String> {
        val tmp = mutableListOf<String>()
        choiceList.forEach {
            it?.let {
                tmp.add(it)
            }
        }
        return tmp
    }
}

internal data class VoteUserData(
    val pollId: String,
    val selectIndex: List<Int>
)

internal data class VoteData(
    val pollState: Int,
    val pollTitle: String,
    val pollId: String,
    val mode: Int,
    val pollItems: List<String>?,
    val pollDetails: Map<String, VoteDetail>
) {

    fun isMultiMode(): Boolean {
        return mode == VoteMode.Multi.value
    }
}

internal data class VoteDetail(
    val num: Int,
    val percentage: Float
)

internal enum class VoteState(val value: Int) {
    Init(-1),
    End(0),
    Polling(1),

    // 对于学生是已提交答案，对于老师来说是已点击开始投票
    Submitted(2)
}

internal enum class VoteMode(val value: Int) {
    Single(1),
    Multi(2);
}

internal data class VoteStartBody(
    val mode: Int,
    val pollTitle: String,
    val pollItems: MutableList<String>
)

internal data class VoteSubmitBody(val selectIndex: List<Int>)

internal interface VoteService {
    @Headers("Content-Type: application/json")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/polls/start")
    fun start(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Body body: VoteStartBody
    ): Call<HttpBaseRes<Any>>

    @Headers("Content-Type: application/json")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/polls/{pollingId}/users/{userUuid}")
    fun submit(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("pollingId") pollingId: String,
        @Path("userUuid") userUuid: String,
        @Body body: VoteSubmitBody
    ): Call<HttpBaseRes<Any>>

    @Headers("Content-Type: application/json")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/polls/{pollingId}/stop")
    fun end(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("pollingId") pollingId: String
    ): Call<HttpBaseRes<Any>>
}

internal class VoteManager(val appId: String, val roomUuid: String) {
    private val tag = "VoteManager"
    private val voteService = AppRetrofitManager.getService(VoteService::class.java)

    fun startVote(mode: Int, pollingTitle: String, pollingItems: MutableList<String>) {
        val body = VoteStartBody(mode, pollingTitle, pollingItems)
        AppRetrofitManager.exc(voteService.start(appId, roomUuid, body), object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(result: HttpBaseRes<Any>?) {

            }
            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
            }
        })
    }

    fun submit(pollingId: String, userUuid: String, selectIndex: List<Int>) {
        val body = VoteSubmitBody(selectIndex)
        voteService.submit(appId, roomUuid, pollingId, userUuid, body)

        AppRetrofitManager.exc(
            voteService.submit(appId, roomUuid, pollingId, userUuid, body),
            object : HttpCallback<HttpBaseRes<Any>>() {
                override fun onSuccess(result: HttpBaseRes<Any>?) {

                }

                override fun onError(httpCode: Int, code: Int, message: String?) {
                    //super.onError(httpCode, code, message)
                }
            })
    }

    fun endVote(pollingId: String) {
        AppRetrofitManager.exc(
            voteService.end(appId, roomUuid, pollingId),
            object : HttpCallback<HttpBaseRes<Any>>() {
                override fun onSuccess(result: HttpBaseRes<Any>?) {
                }

                override fun onError(httpCode: Int, code: Int, message: String?) {
                    //super.onError(httpCode, code, message)
                }
            })
    }
}

















