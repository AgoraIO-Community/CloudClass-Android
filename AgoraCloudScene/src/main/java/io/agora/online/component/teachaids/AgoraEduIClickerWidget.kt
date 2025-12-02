package io.agora.online.component.teachaids

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.*
import io.agora.online.component.teachaids.IClickerAnswerState.answering
import io.agora.online.component.teachaids.IClickerAnswerState.end
import io.agora.online.component.teachaids.TeachAidStatics.EXTRA_KEY_APPID
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors.ResponseIsEmpty
import io.agora.agoraeducore.core.internal.base.http.AppRetrofitManager
import io.agora.agoraeducore.core.internal.education.impl.network.HttpBaseRes
import io.agora.agoraeducore.core.internal.education.impl.network.HttpCallback
import io.agora.agoraeducore.core.internal.framework.data.EduBaseUserInfo
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.log.LogX
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.online.R
import io.agora.online.databinding.FcrOnlineIclickerAnswerResultBinding
import io.agora.online.databinding.FcrOnlineIclickerStudentBinding
import io.agora.online.databinding.FcrOnlineIclickerTeacherBinding
import io.agora.online.databinding.FcrOnlineIclickerWidgetContentBinding
import retrofit2.Call
import retrofit2.http.*
import java.util.*

/**
 * author : cjw
 * date : 2022/2/14
 * description : 答题器
 */
class AgoraTeachAidIClickerWidget : AgoraTeachAidMovableWidget() {
    override val TAG = "AgoraIClickerWidget"

    private var iClickerContent: AgoraIClickerWidgetContent? = null
    private var isFirstEnter = true

    override fun init(container: ViewGroup) {
        super.init(container)
        isFirstEnter = true
        container.post {
            widgetInfo?.localUserInfo?.let {
                iClickerContent = AgoraIClickerWidgetContent(container, it)
                LogX.i(TAG, "创建答题器, $iClickerContent")
            }
        }
    }

    fun getWidgetMsgObserver(): AgoraWidgetMessageObserver? {
        return null
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>, operator: EduBaseUserInfo?
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys, operator)
        iClickerContent?.parseWidgetRoomProperties(properties, widgetInfo?.localUserProperties, operator)
    }

    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
        iClickerContent?.parseWidgetRoomProperties(properties, widgetInfo?.localUserProperties, operator = null)
    }

    override fun release() {
        iClickerContent?.dispose()
        super.release()
    }

    /**
     * 答题器的UI和功能实现
     * iClicker's UI and function implementation
     */
    internal inner class AgoraIClickerWidgetContent(
        val container: ViewGroup,
        val localUserInfo: AgoraWidgetUserInfo
    ) {
        private val tag = "AgoraIClickerWidgetContent"

        // widget's ui has be added to container in here，not repeat add
        private val binding = FcrOnlineIclickerWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )
        private val studentBinding = FcrOnlineIclickerStudentBinding.inflate(
            LayoutInflater.from(container.context),
            binding.iclickerContent, true
        )
        private val answerResultBinding = FcrOnlineIclickerAnswerResultBinding.inflate(
            LayoutInflater.from(container.context),
            binding.iclickerContent, true
        )
        private val teacherBinding = FcrOnlineIclickerTeacherBinding.inflate(
            LayoutInflater.from(container.context),
            binding.iclickerContent, true
        )
        private lateinit var mAnswerAdapter: AnswerAdapter
        private var mTimer: Timer? = null

        private var mSubmitted = false
        private var mStartTime: Long = 0
        private var mTickCount: Long = 0

        // teacher
        private val defaultAnswerCount = 4
        private lateinit var answerItemMax: MutableList<String>
        private lateinit var presetAnswerAdapter: AnswerAdapter
        private lateinit var studentResultsAdapter: StudentResultsAdapter
        private val listUpdateCallback = object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
                ContextCompat.getMainExecutor(teacherBinding.root.context).execute {
                    studentResultsAdapter.notifyItemRangeInserted(position, count)
                }
            }

            override fun onRemoved(position: Int, count: Int) {
                ContextCompat.getMainExecutor(teacherBinding.root.context).execute {
                    studentResultsAdapter.notifyItemRangeRemoved(position, count)
                }
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
                ContextCompat.getMainExecutor(teacherBinding.root.context).execute {
                    studentResultsAdapter.notifyItemMoved(fromPosition, toPosition)
                }
            }

            override fun onChanged(position: Int, count: Int, payload: Any?) {
                (teacherBinding.studentResultsRecyclerView.findViewHolderForAdapterPosition(position)
                    as? StudentAnswerHolder)?.let { holder ->
                    payload?.let { payload ->
                        if (payload is Pair<*, *>) {
                            (payload.second as? IClickerSelectorDetail)?.let { item ->
                                holder.bind(item)
                            }
                        }
                    }
                }
            }
        }
        private val replyItemMatcher = StudentResultItemMatcher()
        private val differ = AsyncListDiffer(listUpdateCallback, AsyncDifferConfig.Builder(replyItemMatcher).build())

        private var iClickerData: IClickerData? = null
        private var iClickerUserData: IClickerUserData? = null
        private var refreshStudentAnswerList = true
        private var globalNextId: Int? = null
        private var studentAnswerCount: Int? = null
        private var iClickerStateManager: IClickerStateManager? = null
        private val loadMoreInterval = 5
        private var myLocalAnswers = mutableListOf<String>()

        init {
            widgetInfo?.let { info ->
                if (info.roomProperties?.isNotEmpty() == true) {
                    parseWidgetRoomProperties(info.roomProperties!!, info.localUserProperties, operator = null)
                }
                info.extraInfo?.let {
                    val appId = (it as? Map<*, *>)?.get(EXTRA_KEY_APPID)
                    if (appId != null && appId.toString().isNotEmpty()) {
                        iClickerStateManager = IClickerStateManager(appId.toString(), widgetInfo!!.roomInfo.roomUuid)
                    } else {
                        LogX.e(tag, "appId is empty, please check widgetInfo.extraInfo")
                    }
                }
            }
            // operate ui follow localUser's role
            readyUIByRole(container.context)
        }

        fun dispose() {
            resetTimer()
            container.removeView(binding.root)
        }

        @Synchronized
        fun parseWidgetRoomProperties(properties: Map<String, Any>, userProperties: Map<String, Any>?, operator: EduBaseUserInfo?) {
            iClickerUserData = if (userProperties?.isNotEmpty() == true) {
                val iClickerUserDataJson = GsonUtil.toJson(userProperties)
                GsonUtil.jsonToObject<IClickerUserData>(iClickerUserDataJson)
            } else {
                null
            }
            val iClickerDataBackup: IClickerData? = try {
                iClickerData?.copy()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            val iClickerDataJson = GsonUtil.toJson(properties)
            LogX.i(TAG, "答题器数据：$iClickerDataJson")
            iClickerData = iClickerDataJson.let { GsonUtil.jsonToObject<IClickerData>(it) }
            LogX.i(TAG, "答题器数据 iClickerData?.answerState = ${iClickerData?.answerState}")

            // handle UI and dataState with iClickerData
            if (mSubmitted && iClickerData?.answerState == answering && iClickerDataBackup?.answerState == end) {
                mSubmitted = false
            }

            refreshStudentAnswerList = true
            when (iClickerData?.answerState) {
                answering -> {
                    switchTimer(properties, true)
                    handleAnswering(operator)
                }
                end -> {
                    switchTimer(properties, false)
                    handleAnswerEnd()
                }
            }

            ContextCompat.getMainExecutor(binding.root.context).execute {
                if (iClickerData?.answerState == IClickerAnswerState.init) {  // hidden ui
                    binding.root.visibility = View.GONE
                    readyUIByRole(container.context)
                    switchTimer(properties, false)
                    handleAnswering(operator)
                    iClickerUserData = null
                    myLocalAnswers.clear()
                    submitOrModifyAnswers(false)
                } else { // show ui
                    binding.root.visibility = View.VISIBLE
                }
            }
        }

        /**
         * refresh ui follow answering state
         */
        private fun handleAnswering(operator: EduBaseUserInfo? = null) {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (isTeacher) {
                showAnswerResult(false)
            } else {
                if (iClickerData != null) {
                    LogX.i(tag, "IClicker started: start time:" + iClickerData?.receiveQuestionTime
                        + ", allItems:" + iClickerData?.items?.let { GsonUtil.toJson(it) })
                    showAnswerItems(iClickerData!!.items, operator)
                }
            }
        }

        private fun handleAnswerEnd() {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (isTeacher) {
                showAnswerResult(true)
            } else {
                LogX.i("IClicker(student) end: " + iClickerData?.let { GsonUtil.toJson(it) })
                showStudentResult()
            }
        }

        private fun onSubmitClick() {
            if (!studentBinding.submitBtn.isSelected) {
                submitProperties()
            }
            submitOrModifyAnswers(!studentBinding.submitBtn.isSelected)
        }

        private fun submitOrModifyAnswers(submit: Boolean) {
            if (submit) {
                mSubmitted = true
            }

//            studentBinding.submitBtn.isSelected = mAnswerAdapter.getCheckedItems().size != 0
            studentBinding.submitBtn.isSelected = submit //selected 不应该由submit状态判断
            studentBinding.submitBtn.setText(if (submit) R.string.fcr_popup_quiz_change else R.string.fcr_popup_quiz_post)
            mAnswerAdapter.setEnabled(!submit)
            for (view in studentBinding.answersGridView.children) {
                view.isEnabled = !submit
            }
        }

        private fun submitProperties() {
            myLocalAnswers = mAnswerAdapter.getCheckedItems()
                .sorted()
                .map { ('A' + it).toString() }
                .toMutableList()
            iClickerData?.let {
                iClickerStateManager?.submitAnswer(
                    localUserInfo.userUuid,//local user info
                    it.popupQuizId,
                    myLocalAnswers,
                    it.receiveQuestionTime
                )
                return
            }
            LogX.w(tag, "iClickerData is empty, please checkout widgetRoomProperties")
        }

        // show answer items for student to choose
        private fun showAnswerItems(answers: List<String>, operator: EduBaseUserInfo? = null) {
            ContextCompat.getMainExecutor(studentBinding.root.context).execute {
                studentBinding.studentLayout.visibility = VISIBLE
                answerResultBinding.studentAnswerResultLayout.visibility = GONE
                mAnswerAdapter.dataList = answers.toMutableList()
                LogX.i(tag, "showAnswerItems operator?.userUuid=${operator?.userUuid}")
                // 退出教室重新进入，返回的userUuid是错误的
                if (isFirstEnter || localUserInfo.userUuid == operator?.userUuid) {
                    isFirstEnter = false
                    //其他学生提交、修改答案 不影响本地用户的答题状态
                    val alreadySubmit = iClickerUserData?.popupQuizId == iClickerData?.popupQuizId
                        && iClickerUserData?.selectedItems?.isNotEmpty() == true
                    studentBinding.submitBtn.isEnabled = alreadySubmit
                    submitOrModifyAnswers(alreadySubmit)
                    if (alreadySubmit) {
                        iClickerUserData?.selectedItems?.map { it.toCharArray()[0] }?.map { it - 'A' }?.let {
                            mAnswerAdapter.setChecked(it)
                        }
                    } else {
                        mAnswerAdapter.setChecked(mutableListOf())
                    }
                }
                mAnswerAdapter.notifyDataSetChanged()
            }
        }

        private fun showStudentResult() {
            ContextCompat.getMainExecutor(binding.root.context).execute {
                binding.timerText.isEnabled = false
                studentBinding.studentLayout.visibility = GONE
                answerResultBinding.studentAnswerResultLayout.visibility = VISIBLE

                if (iClickerData != null || iClickerUserData != null) {
                    val correctAnswerString = iClickerData!!.correctItems.sorted().reduce { acc, s -> acc + s }
                    val respondents = String.format(
                        container.resources.getString(R.string.fcr_respondentsnum),
                        iClickerData!!.selectedCount, iClickerData!!.totalCount
                    )
                    val accuracy = String.format(
                        container.resources.getString(R.string.fcr_accuracynum),
                        (iClickerData!!.averageAccuracy * 100).toInt()
                    )
                    answerResultBinding.numberOfRespondents.text = respondents
                    answerResultBinding.accuracy.text = accuracy
                    answerResultBinding.correctAnswers.text = correctAnswerString
                    var myAnswers = ""
                    myLocalAnswers.forEach {
                        myAnswers = myAnswers.plus(it)
                    }
                    answerResultBinding.myAnswers.text = myAnswers
                    answerResultBinding.myAnswers.isSelected = correctAnswerString == myAnswers
                }
            }
        }

        // show the results of answer for the teacher
        private fun showAnswerResult(end: Boolean) {
            ContextCompat.getMainExecutor(teacherBinding.root.context).execute {
                teacherBinding.presetLayout.visibility = GONE
                teacherBinding.answeringLayout.visibility = VISIBLE
                binding.timerText.isEnabled = false

                if (iClickerData != null) {
                    val correctAnswerString = iClickerData!!.correctItems.sorted().reduce { acc, s -> acc + s }
                    val respondents = String.format(
                        container.resources.getString(R.string.fcr_respondentsnum),
                        iClickerData!!.selectedCount, iClickerData!!.totalCount
                    )
                    val accuracy = String.format(
                        container.resources.getString(R.string.fcr_accuracynum),
                        (iClickerData!!.averageAccuracy * 100).toInt()
                    )
                    teacherBinding.respondentsTextView.text = respondents
                    teacherBinding.accuracyTextView.text = accuracy
                    teacherBinding.correctAnswersTextView.text = correctAnswerString
                    teacherBinding.actionAnswer.text = container.resources.getString(
                        if (end) R.string.fcr_popup_quiz_start_again else R.string.fcr_popup_quiz_end_answer
                    )
                    // set correctAnswer to studentResultsAdapter
                    studentResultsAdapter.notifyCorrectAnswer(iClickerData!!.correctItems.sorted().toMutableList())
                    if (end) {
                        // todo, hide restart function by this time
                        teacherBinding.actionAnswer.visibility = GONE
                    }
                    if (refreshStudentAnswerList) {
                        refreshStudentAnswerList = false
                        // pull and show student answer result when end.
                        loadMoreStudentResult(count = Int.MAX_VALUE)
                    }
                }
            }
        }

        private fun switchTimer(properties: Map<String, Any?>, timerRunning: Boolean) {
            mStartTime = iClickerData?.receiveQuestionTime ?: 0L
            if (timerRunning) {
                val tick = TimeUtil.currentTimeMillis() - mStartTime
                if (tick >= 0) {
                    mTickCount = tick / 1000L
                }
                resetTimer()
                mTimer = Timer()
                mTimer?.schedule(object : TimerTask() {
                    override fun run() {
                        mTickCount++
                        ContextCompat.getMainExecutor(binding.root.context).execute {
                            binding.timerText.text = TimeUtil.stringForTimeHMS(mTickCount, "%02d:%02d:%02d")
                        }
                    }
                }, 1000, 1000)
            } else {
                val endTime = iClickerUserData?.lastCommitTime ?: 0L
                ContextCompat.getMainExecutor(binding.root.context).execute {
                    binding.timerText.text = TimeUtil.stringForTimeHMS(endTime - mStartTime, "%02d:%02d:%02d")
                }
                resetTimer()
            }
            ContextCompat.getMainExecutor(binding.root.context).execute {
                binding.timerText.visibility = if (timerRunning) VISIBLE else GONE
            }
        }

        private fun resetTimer() {
            if (mTimer != null) {
                mTimer?.purge()
                mTimer?.cancel()
                mTimer = null
            }
        }

        private fun readyUIByRole(context: Context) {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (!isTeacher) {
                studentBinding.studentLayout.visibility = VISIBLE
                teacherBinding.teacherLayout.visibility = GONE
                if (localUserInfo.userRole == AgoraEduContextUserRole.Observer.value) {
                    studentBinding.submitBtn.visibility = GONE
                }
                studentBinding.submitBtn.setOnClickListener { onSubmitClick() }
                mAnswerAdapter = AnswerAdapter(context, mutableListOf())
                studentBinding.answersGridView.adapter = mAnswerAdapter
                mAnswerAdapter.setOnCheckedTextView(object : AnswerAdapter.OnCheckChangedListener {
                    override fun onChanged(isChecked: Boolean) {
                        studentBinding.submitBtn.isEnabled = isChecked
                    }
                })
            } else {
                binding.closeImg.visibility = VISIBLE
                studentBinding.studentLayout.visibility = GONE
                teacherBinding.teacherLayout.visibility = VISIBLE
                answerItemMax = context.resources.getStringArray(R.array.answer_item).toMutableList()
                binding.closeImg.setOnClickListener {
                    onTeacherSwitchSelf()
                }
                teacherBinding.plusImg.setOnClickListener {
                    presetAnswerAdapter.let {
                        if (it.count < answerItemMax.size) {
                            it.modifyData(answerItemMax[it.count], true)
                        }
                        if (it.count >= answerItemMax.size) {
                            teacherBinding.plusImg.visibility = View.INVISIBLE
                        }
                        if (it.count > defaultAnswerCount / 2) {
                            teacherBinding.surplusImg.visibility = VISIBLE
                        }
                    }
                }
                teacherBinding.startAnswer.setOnClickListener {
//                presetLayout.visibility = GONE
//                answeringLayout.visibility = VISIBLE
                    startAnswer()
                }
                teacherBinding.surplusImg.setOnClickListener {
                    presetAnswerAdapter.let {
                        if (it.count > defaultAnswerCount / 2) {
                            presetAnswerAdapter.modifyData(add = false)
                        }
                        if (it.count <= defaultAnswerCount / 2) {
                            teacherBinding.surplusImg.visibility = View.INVISIBLE
                        }
                        if (it.count < answerItemMax.size) {
                            teacherBinding.plusImg.visibility = VISIBLE
                        }
                        teacherBinding.startAnswer.isEnabled = it.getCheckedItems().isNotEmpty()
                    }
                }
                teacherBinding.actionAnswer.setOnClickListener {
                    when (iClickerData?.answerState) {
                        answering -> {
                            stopAnswer()
                            resetTimer()
                            // todo, hide restart function by this time
//                            teacherBinding.actionAnswer.text = context.resources.getString(R.string.start_again)
                        }
                        end -> {
                            refreshPresetAnswer(context)
                            restartAnswer()
                            teacherBinding.actionAnswer.text =
                                context.resources.getString(R.string.fcr_popup_quiz_end_answer)
                        }
                    }
                }
                refreshPresetAnswer(context)
                readyAnsweringUI(context)
            }
        }

        private fun refreshPresetAnswer(context: Context) {
            val data = mutableListOf<String>()
            data.addAll(answerItemMax.subList(0, defaultAnswerCount))
            presetAnswerAdapter = AnswerAdapter(context, data)
            teacherBinding.presetAnswersGridview.adapter = presetAnswerAdapter
            presetAnswerAdapter.setOnCheckedTextView(object : AnswerAdapter.OnCheckChangedListener {
                override fun onChanged(isChecked: Boolean) {
                    teacherBinding.startAnswer.isEnabled = isChecked
                }
            })
            teacherBinding.startAnswer.isEnabled = false
        }

        /**
         * close current widget（call IClickerService.delIClicker）
         */
        private fun onTeacherSwitchSelf(close: Boolean = true, extraProperties: Map<String, Any>? = null) {
            val packet = AgoraTeachAidWidgetInteractionPacket(
                AgoraTeachAidWidgetInteractionSignal.ActiveState,
                AgoraTeachAidWidgetActiveStateChangeData(!close, extraProperties)//发送关闭答题器widget的消息给其他端
            )
            val json = GsonUtil.toJson(packet)
            json?.let { sendMessage(it) }
        }

        private fun readyAnsweringUI(context: Context) {
            val lm = object : LinearLayoutManager(context, VERTICAL, false) {
                override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                    try {
                        super.onLayoutChildren(recycler, state)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            teacherBinding.studentResultsRecyclerView.layoutManager = lm
            studentResultsAdapter = StudentResultsAdapter(mutableListOf(), differ)
            teacherBinding.studentResultsRecyclerView.adapter = studentResultsAdapter
//            teacherBinding.studentResultsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                    super.onScrollStateChanged(recyclerView, newState)
//                    val lastIndex = differ.currentList.size - 1
//                    val lastVisibleIndex = lm.findLastVisibleItemPosition()
//                    if (lastIndex - lastVisibleIndex <= loadMoreInterval && studentAnswerCount != null
//                        && differ.currentList.size < studentAnswerCount!!) {
//                        LogX.i(tag,"加载更多数据")
//                        loadMoreStudentResult(curNextId = globalNextId)
//                    }
//                }
//
//                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                    super.onScrolled(recyclerView, dx, dy)
//                }
//            })
        }

        private fun startAnswer() {
            val correctItems = presetAnswerAdapter.getCheckedItems()
                .sorted()
                .map { answerItemMax[it] }
                .toMutableList()
            val allItems = presetAnswerAdapter.dataList
            iClickerStateManager?.start(allItems, correctItems)
        }

        private fun stopAnswer() {
            iClickerData?.let {
                iClickerStateManager?.end(it.popupQuizId)
                return
            }
            LogX.w(tag, "iClickerData is empty, please checkout widgetRoomProperties")
        }

        /**
         * reStart answer, this is local operate
         */
        private fun restartAnswer() {
            teacherBinding.presetLayout.visibility = VISIBLE
            teacherBinding.answeringLayout.visibility = GONE
            // clear studentAnswerResult list
            differ.submitList(mutableListOf())
        }

        private fun loadMoreStudentResult(curNextId: Int? = null, count: Int? = null) {
            LogX.i(tag,"loadMoreStudentResult-curNextId:$curNextId")
            iClickerData?.let {
                if (TextUtils.isEmpty(it.popupQuizId)) {
                    LogX.w(tag, "popupQuizId:$it.popupQuizId")
                    return
                }

                iClickerStateManager?.pullAnswerResult(selectorId = it.popupQuizId, nextId = curNextId, count = count,
                    callback = object : EduContextCallback<IClickerAnswerResultRes?> {
                        override fun onSuccess(target: IClickerAnswerResultRes?) {
                            LogX.i(tag, "loadMoreStudentResult success")
                            studentAnswerCount = target?.total
                            globalNextId = target?.nextId
                            target?.list?.let { detailList ->
                                LogX.i(tag, "loadMoreStudentResult success, notify recyclerView")
                                val newList = mutableListOf<IClickerSelectorDetail>()
                                newList.addAll(differ.currentList)
                                detailList.forEach continuing@{ newItem ->
                                    newItem.receiveQuestionTime = iClickerData?.receiveQuestionTime
                                    differ.currentList.forEachIndexed { index, oldItem ->
                                        if (newItem.ownerUserUuid == oldItem.ownerUserUuid) {
                                            newList.removeAt(index)
                                            newList.add(index, newItem)
                                            return@continuing
                                        }
                                    }
                                    newList.add(newItem)
                                }
                                differ.submitList(newList)
                            }
                        }

                        override fun onFailure(error: EduContextError?) {
                            LogX.e(tag, "pullAnswerResult error:${error.toString()}")
                        }
                    })
            }
        }
    }
}

internal class AnswerAdapter(var context: Context, var dataList: MutableList<String>) : BaseAdapter() {
    private val checkedSets = mutableSetOf<Int>()
    private var onCheckChangedListener: OnCheckChangedListener? = null
    private var isEnabled = true

    inner class ViewHolder {
        lateinit var textView: CheckedTextView
    }

    interface OnCheckChangedListener {
        fun onChanged(isChecked: Boolean)
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewHolder = ViewHolder()
        val view = LayoutInflater.from(context).inflate(R.layout.fcr_online_iclicker_answer_preset_item, parent, false)
        viewHolder.textView = view.findViewById(R.id.iclicker_answer_item_text)
        viewHolder.textView.setOnClickListener {
            val contain = isCheckedPosition(position)
            viewHolder.textView.isChecked = !contain
            if (contain) {
                checkedSets.remove(position)
            } else {
                checkedSets.add(position)
            }
            onCheckChangedListener?.onChanged(isChecked())
        }
        view.tag = viewHolder

        viewHolder.textView.text = dataList[position]
        viewHolder.textView.isChecked = isCheckedPosition(position)
        viewHolder.textView.isEnabled = isEnabled

        return view!!
    }

    override fun getItem(position: Int) = dataList[position]

    override fun getItemId(position: Int) = position.toLong()

    override fun getCount() = dataList.size

    fun modifyData(item: String? = null, add: Boolean) {
        if (add) {
            item?.let {
                dataList.add(it)
            }
        } else {
            val index = count - 1
            if (checkedSets.contains(index)) {
                checkedSets.remove(index)
            }
            dataList.removeAt(index)
        }
        notifyDataSetChanged()
    }

    fun setChecked(selected: List<Int>) {
        checkedSets.clear()
        checkedSets.addAll(selected)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    private fun isCheckedPosition(position: Int): Boolean {
        if (checkedSets.isEmpty()) {
            return false
        }
        return checkedSets.indexOf(position) != -1
    }

    private fun isChecked() = checkedSets.isNotEmpty()

    fun getCheckedItems() = checkedSets

    fun setOnCheckedTextView(listener: OnCheckChangedListener) {
        onCheckChangedListener = listener
    }
}

internal class StudentResultsAdapter(
    private var correctAnswers: MutableList<String>,
    private val differ: AsyncListDiffer<IClickerSelectorDetail>
) :
    RecyclerView.Adapter<StudentAnswerHolder>() {

    fun notifyCorrectAnswer(data: MutableList<String>) {
        this.correctAnswers.clear()
        this.correctAnswers.addAll(data)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentAnswerHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fcr_online_iclicker_answer_list_item, parent, false)
        return StudentAnswerHolder(view, correctAnswers)
    }

    override fun onBindViewHolder(holder: StudentAnswerHolder, position: Int) {
        holder.bind(differ.currentList[holder.absoluteAdapterPosition])
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}

internal class StudentResultItemMatcher : DiffUtil.ItemCallback<IClickerSelectorDetail>() {
    override fun areItemsTheSame(oldItem: IClickerSelectorDetail, newItem: IClickerSelectorDetail): Boolean {
        return (oldItem.ownerUserUuid == newItem.ownerUserUuid)
    }

    override fun areContentsTheSame(oldItem: IClickerSelectorDetail, newItem: IClickerSelectorDetail): Boolean {
        return (oldItem.ownerUserUuid == newItem.ownerUserUuid
            && oldItem.ownerUserName == newItem.ownerUserName
            && oldItem.selectedItems == newItem.selectedItems
            && oldItem.lastCommitTime == newItem.lastCommitTime
            && oldItem.selectorId == newItem.selectorId
            && oldItem.isCorrect == newItem.isCorrect)
    }

    override fun getChangePayload(oldItem: IClickerSelectorDetail, newItem: IClickerSelectorDetail): Any {
        return Pair(oldItem, newItem)
    }
}

@SuppressLint("SimpleDateFormat")
internal class StudentAnswerHolder(view: View, private val correctAnswers: MutableList<String>) :
    RecyclerView.ViewHolder(view) {
    private val name: AppCompatTextView = view.findViewById(R.id.name)
    private val time: AppCompatTextView = view.findViewById(R.id.time)
    private val answers: AppCompatTextView = view.findViewById(R.id.answers_gridView)

    fun bind(item: IClickerSelectorDetail) {
        itemView.post {
            name.text = item.ownerUserName
            val curAnswerList = item.selectedItems.sorted().toMutableList()
            val color: Int = if (curAnswerList == correctAnswers) {
                itemView.resources.getColor(R.color.iclicker_answer_list_item_correct)
            } else {
                itemView.resources.getColor(R.color.iclicker_answer_list_item_error)
            }
            answers.setTextColor(color)
            val correctAnswerString = if (curAnswerList.isNullOrEmpty()) "" else
                curAnswerList.reduce { acc, s -> acc + s }
            answers.text = correctAnswerString
            item.receiveQuestionTime?.let {
                if (it > 0 && item.lastCommitTime > it) {
                    val duration = item.lastCommitTime - it
                    time.text = TimeUtil.stringForTimeHMS(duration / 1000, "%02d:%02d:%02d")
                }
            }
        }
    }
}

/**
 * 答题状态枚举
 * Answer status enumeration
 */
internal object IClickerAnswerState {
    const val answering = 1
    const val end = 0
    const val init = 2 // reset
}

internal data class IClickerData(
    val popupQuizId: String,
    val correctItems: List<String>,
    val items: List<String>,
    val correctCount: Int,
    val averageAccuracy: Float,
    val answerState: Int,
    val receiveQuestionTime: Long,
    val selectedCount: Int,
    val totalCount: Int
) {

    fun copy(): IClickerData {
        return IClickerData(
            popupQuizId, correctItems, items, correctCount, averageAccuracy,
            answerState, receiveQuestionTime, selectedCount, totalCount
        )
    }
}

internal data class IClickerUserData(
    val popupQuizId: String,
    val selectedItems: List<String>,
    val lastCommitTime: Long,
    val isCorrect: Boolean
)

internal data class IClickerSelectorDetail(
    val ownerUserUuid: String,
    val ownerUserName: String,
    val selectedItems: MutableList<String>,
    val lastCommitTime: Long,
    val selectorId: String,
    val isCorrect: Boolean,
    // need to be set up separately
    var receiveQuestionTime: Long?
)

internal class IClickerStateManager(val appId: String, val roomUuid: String) {
    private val tag = "IClickerStateManager"
    private val iClickerService = AppRetrofitManager.getService(IClickerService::class.java)

    fun start(allItems: MutableList<String>, correctItems: MutableList<String>) {
        val body = IClickerStartAnswerBody(allItems, correctItems)
        val call = iClickerService.start(appId, roomUuid, body)
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(result: HttpBaseRes<Any>?) {
                val a = 0
            }

            override fun onError(httpCode: Int, code: Int, message: String?) {
                //super.onError(httpCode, code, message)
                val a = 0
            }
        })
    }

    fun submitAnswer(
        userUuid: String,
        selectorId: String,
        selectedItems: MutableList<String>,
        receiveQuestionTime: Long
    ) {
        val body = IClickerSubmitAnswerBody(selectedItems, receiveQuestionTime)
        val call = iClickerService.submitAnswers(appId, roomUuid, selectorId, userUuid, body)
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(result: HttpBaseRes<Any>?) {
            }
        })
    }

    fun end(selectorId: String) {
        val call = iClickerService.end(appId, roomUuid, selectorId)
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<Any>>() {
            override fun onSuccess(result: HttpBaseRes<Any>?) {
            }
        })
    }

    fun pullAnswerResult(
        selectorId: String, nextId: Int? = null, count: Int? = null,
        callback: EduContextCallback<IClickerAnswerResultRes?>
    ) {
        val call = iClickerService.pullAnswerResult(appId, roomUuid, selectorId, nextId, count)
        AppRetrofitManager.exc(call, object : HttpCallback<HttpBaseRes<IClickerAnswerResultRes>>() {
            override fun onSuccess(result: HttpBaseRes<IClickerAnswerResultRes>?) {
                result?.let {
                    if (!it.data.list.isNullOrEmpty()) {
                        callback.onSuccess(it.data)
                    } else {
                        callback.onSuccess(null)
                    }
                    return
                }
                callback.onFailure(ResponseIsEmpty)
            }
        })
    }
}

internal interface IClickerService {
    /**
     * start answer question
     */
    @Headers("Content-Type: application/json")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/start")
    fun start(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Body body: IClickerStartAnswerBody
    ): Call<HttpBaseRes<Any>>

    @Headers("Content-Type: application/json")
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/{selectorId}/users/{userUuid}")
    fun submitAnswers(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("selectorId") selectorId: String,
        @Path("userUuid") userUuid: String,
        @Body body: IClickerSubmitAnswerBody
    ): Call<HttpBaseRes<Any>>

    /**
     * end answer question
     */
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/{selectorId}/stop")
    fun end(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("selectorId") selectorId: String
    ): Call<HttpBaseRes<Any>>

    /**
     * pull answer result
     */
    @GET("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/{selectorId}/users")
    fun pullAnswerResult(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("selectorId") selectorId: String,
        @Query("nextId") nextId: Int? = null,
        @Query("count") count: Int? = 10
    ): Call<HttpBaseRes<IClickerAnswerResultRes>>
}

internal data class IClickerStartAnswerBody(
    val items: MutableList<String>,
    val correctItems: MutableList<String>
)

internal data class IClickerSubmitAnswerBody(
    val selectedItems: MutableList<String>,
    //  corrected localTime
    val receiveQuestionTime: Long
)

internal data class IClickerAnswerResultRes(
    val total: Int,
    val list: List<IClickerSelectorDetail>?,
    val nextId: Int?,
    val count: Int
) {
}