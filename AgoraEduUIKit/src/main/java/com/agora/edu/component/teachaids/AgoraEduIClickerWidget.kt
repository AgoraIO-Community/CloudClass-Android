package com.agora.edu.component.teachaids

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.recyclerview.widget.*
import com.agora.edu.component.teachaids.IClickerAnswerState.answering
import com.agora.edu.component.teachaids.IClickerAnswerState.end
import com.agora.edu.component.teachaids.TeachAidStatics.EXTRA_KEY_APPID
import io.agora.agoraeducore.core.context.AgoraEduContextUserRole
import io.agora.agoraeducore.core.context.EduContextCallback
import io.agora.agoraeducore.core.context.EduContextError
import io.agora.agoraeducore.core.context.EduContextErrors.ResponseIsEmpty
import io.agora.agoraeducore.core.internal.base.callback.ThrowableCallback
import io.agora.agoraeducore.core.internal.base.network.RetrofitManager
import io.agora.agoraeducore.core.internal.base.network.ResponseBody
import io.agora.agoraeducore.core.internal.education.impl.Constants.AgoraLog
import io.agora.agoraeducore.core.internal.framework.utils.GsonUtil
import io.agora.agoraeducore.core.internal.launch.AgoraEduSDK
import io.agora.agoraeducore.core.internal.util.TimeUtil
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetMessageObserver
import io.agora.agoraeducore.extensions.widgets.bean.AgoraWidgetUserInfo
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.databinding.AgoraIclickerAnswerResultBinding
import io.agora.agoraeduuikit.databinding.AgoraIclickerStudentBinding
import io.agora.agoraeduuikit.databinding.AgoraIclickerTeacherBinding
import io.agora.agoraeduuikit.databinding.AgoraIclickerWidgetContentBinding
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal.BoardGrantDataChanged
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import retrofit2.Call
import retrofit2.http.*
import java.util.*

/**
 * author : cjw
 * date : 2022/2/14
 * description :
 */
class AgoraTeachAidIClickerWidget : AgoraTeachAidMovableWidget() {
    override val tag = "AgoraIClickerWidget"

    private var iClickerContent: AgoraIClickerWidgetContent? = null

    init {
    }

    fun getWidgetMsgObserver(): AgoraWidgetMessageObserver? {
        return iClickerContent?.whiteBoardObserver
    }

    override fun init(container: ViewGroup) {
        super.init(container)
        container.post {
            widgetInfo?.localUserInfo?.let {
                iClickerContent = AgoraIClickerWidgetContent(container, it)
            }
        }
    }

    override fun onWidgetRoomPropertiesUpdated(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesUpdated(properties, cause, keys)
        iClickerContent?.parseWidgetRoomProperties(properties, widgetInfo?.localUserProperties)
    }

    override fun onWidgetRoomPropertiesDeleted(
        properties: MutableMap<String, Any>, cause: MutableMap<String, Any>?,
        keys: MutableList<String>
    ) {
        super.onWidgetRoomPropertiesDeleted(properties, cause, keys)
        iClickerContent?.parseWidgetRoomProperties(properties, widgetInfo?.localUserProperties)
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
        private val binding = AgoraIclickerWidgetContentBinding.inflate(
            LayoutInflater.from(container.context),
            container, true
        )
        private val studentBinding = AgoraIclickerStudentBinding.inflate(
            LayoutInflater.from(container.context),
            binding.iclickerContent, true
        )
        private val answerResultBinding = AgoraIclickerAnswerResultBinding.inflate(
            LayoutInflater.from(container.context),
            binding.iclickerContent, true
        )
        private val teacherBinding = AgoraIclickerTeacherBinding.inflate(
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
        val whiteBoardObserver = object : AgoraWidgetMessageObserver {
            override fun onMessageReceived(msg: String, id: String) {
                val packet = GsonUtil.jsonToObject<AgoraBoardInteractionPacket>(msg)
                if (packet?.signal == BoardGrantDataChanged) {
                    val granted = (packet?.body as? ArrayList<*>)?.contains(localUserInfo.userUuid) ?: false
                    // todo
//                    setDraggable(granted)
                }
            }
        }
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

        // data of the iClicker itself
        private var iClickerData: IClickerData? = null

        // data of localUser's iClicker
        private var iClickerUserData: IClickerUserData? = null

        // whether refresh the list of student answer results
        private var refreshStudentAnswerList = true

        // data of cur studentAnswers nextId and all studentAnswer count
        private var globalNextId: Int? = null
        private var studentAnswerCount: Int? = null
        private var iClickerStateManager: IClickerStateManager? = null
        private val loadMoreInterval = 5

        init {
            widgetInfo?.let { info ->
                if (info.roomProperties?.isNotEmpty() == true) {
                    parseWidgetRoomProperties(info.roomProperties!!, info.localUserProperties)
                }
                info.extraInfo?.let {
                    val appId = (it as? Map<*, *>)?.get(EXTRA_KEY_APPID)
                    if (appId != null && appId.toString().isNotEmpty()) {
                        iClickerStateManager = IClickerStateManager(appId.toString(), widgetInfo!!.roomInfo.roomUuid)
                    } else {
                        AgoraLog?.e("$tag->appId is empty, please check widgetInfo.extraInfo")
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
        fun parseWidgetUserProperties() {
        }

        @Synchronized
        fun parseWidgetRoomProperties(properties: Map<String, Any>, userProperties: Map<String, Any>?) {
            iClickerUserData = if (userProperties?.isNotEmpty() == true) {
                val iClickerUserDataJson = GsonUtil.toJson(userProperties)
                iClickerUserDataJson?.let { GsonUtil.jsonToObject<IClickerUserData>(it) }
            } else {
                null
            }
            val iClickerDataBackup = iClickerData?.copy()
            // parse widget's properties, and convert to iClickerData
            val iClickerDataJson = GsonUtil.toJson(properties)
            iClickerData = iClickerDataJson?.let { GsonUtil.jsonToObject<IClickerData>(it) }
            // handle UI and dataState with iClickerData
            if (mSubmitted && iClickerData?.answerState == answering && iClickerDataBackup?.answerState == end) {
                mSubmitted = false
            }
            // if student answer data has been changed, need refresh the list of student answer results
            // todo refresh all when properties changed
//            if (iClickerData?.averageAccuracy != iClickerDataBackup?.averageAccuracy
//                || iClickerData?.correctCount != iClickerDataBackup?.correctCount
//                || differ.currentList.isEmpty()
//            ) {
//                refreshStudentAnswerList = true
//            }
            refreshStudentAnswerList = true
            when (iClickerData?.answerState) {
                answering -> {
                    switchTimer(properties, true)
                    handleAnswering()
                }
                end -> {
                    switchTimer(properties, false)
                    handleAnswerEnd()
                }
            }
        }

        /**
         * refresh ui follow answering state
         */
        private fun handleAnswering() {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (isTeacher) {
                showAnswerResult(false)
            } else {
                if (iClickerData != null) {
                    AgoraLog?.i("$tag->IClicker started: start time:" + iClickerData?.receiveQuestionTime
                        + ", allItems:" + iClickerData?.items?.let { GsonUtil.toJson(it) })
                    showAnswerItems(iClickerData!!.items)
                }
            }
        }

        private fun handleAnswerEnd() {
            val isTeacher = localUserInfo.userRole == AgoraEduContextUserRole.Teacher.value
            if (isTeacher) {
                showAnswerResult(true)
            } else {
                AgoraLog?.i("IClicker(student) end: " + iClickerData?.let { GsonUtil.toJson(it) })
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
            studentBinding.submitBtn.isSelected = submit
            studentBinding.submitBtn.setText(if (submit) R.string.fcr_popup_quiz_change else R.string.fcr_popup_quiz_post)
            mAnswerAdapter.setEnabled(!submit)
            for (view in studentBinding.answersGridView.children) {
                view.isEnabled = !submit
            }
        }

        private fun submitProperties() {
            val myAnswers = mAnswerAdapter.getCheckedItems()
                .sorted()
                .map { ('A' + it).toString() }
                .toMutableList()
            iClickerData?.let {
                iClickerStateManager?.submitAnswer(
                    localUserInfo.userUuid,
                    it.popupQuizId,
                    myAnswers,
                    it.receiveQuestionTime
                )
                return
            }
            AgoraLog?.w("$tag->iClickerData is empty, please checkout widgetRoomProperties")
        }

        // show answer items for student to choose
        private fun showAnswerItems(answers: List<String>) {
            ContextCompat.getMainExecutor(studentBinding.root.context).execute {
                studentBinding.studentLayout.visibility = VISIBLE
                answerResultBinding.studentAnswerResultLayout.visibility = GONE

                mAnswerAdapter.dataList = answers.toMutableList()
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
                mAnswerAdapter.notifyDataSetChanged()
            }
        }

        private fun showStudentResult() {
            ContextCompat.getMainExecutor(binding.root.context).execute {
                binding.timerText.isEnabled = false
                studentBinding.studentLayout.visibility = GONE
                answerResultBinding.studentAnswerResultLayout.visibility = VISIBLE

                if (iClickerData != null && iClickerUserData != null) {
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
                    val myAnswers = iClickerUserData!!.selectedItems.joinToString("")
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
//                        AgoraLog?.i("$tag->加载更多数据")
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
            AgoraLog?.w("$tag->iClickerData is empty, please checkout widgetRoomProperties")
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
            AgoraLog?.i("tag->loadMoreStudentResult-curNextId:$curNextId")
            iClickerData?.let {
                iClickerStateManager?.pullAnswerResult(selectorId = it.popupQuizId, nextId = curNextId, count = count,
                    callback = object : EduContextCallback<IClickerAnswerResultRes?> {
                        override fun onSuccess(target: IClickerAnswerResultRes?) {
                            AgoraLog?.i("$tag->loadMoreStudentResult success")
                            studentAnswerCount = target?.total
                            globalNextId = target?.nextId
                            target?.list?.let { detailList ->
                                AgoraLog?.i("$tag->loadMoreStudentResult success, notify recyclerView")
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
                            AgoraLog?.e("$tag->pullAnswerResult error:${error.toString()}")
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
        val view = LayoutInflater.from(context).inflate(R.layout.fcr_iclicker_answer_preset_item, parent, false)
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.fcr_iclicker_answer_list_item, parent, false)
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
    private val iClickerService =
        RetrofitManager.instance().getService(AgoraEduSDK.baseUrl(), IClickerService::class.java)

    fun start(allItems: MutableList<String>, correctItems: MutableList<String>) {
        val body = IClickerStartAnswerBody(allItems, correctItems)
        iClickerService.start(appId, roomUuid, body)
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                override fun onSuccess(res: ResponseBody<String>?) {
                }

                override fun onFailure(throwable: Throwable?) {
                    AgoraLog?.e("$tag->${throwable?.let { GsonUtil.toJson(it) }}")
                }
            }))
    }

    fun submitAnswer(
        userUuid: String,
        selectorId: String,
        selectedItems: MutableList<String>,
        receiveQuestionTime: Long
    ) {
        val body = IClickerSubmitAnswerBody(selectedItems, receiveQuestionTime)
        iClickerService.submitAnswers(appId, roomUuid, selectorId, userUuid, body)
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                override fun onSuccess(res: ResponseBody<String>?) {
                }

                override fun onFailure(throwable: Throwable?) {
                    AgoraLog?.e("$tag->${throwable?.let { GsonUtil.toJson(it) }}")
                }
            }))
    }

    fun end(selectorId: String) {
        iClickerService.end(appId, roomUuid, selectorId)
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<ResponseBody<String>> {
                override fun onSuccess(res: ResponseBody<String>?) {
                }

                override fun onFailure(throwable: Throwable?) {
                    AgoraLog?.e("$tag->${throwable?.let { GsonUtil.toJson(it) }}")
                }
            }))
    }

    fun pullAnswerResult(
        selectorId: String, nextId: Int? = null, count: Int? = null,
        callback: EduContextCallback<IClickerAnswerResultRes?>
    ) {
        iClickerService.pullAnswerResult(appId, roomUuid, selectorId, nextId, count)
            .enqueue(RetrofitManager.Callback(0, object : ThrowableCallback<io.agora.agoraeducore.core.internal.
            edu.common.bean.ResponseBody<IClickerAnswerResultRes>> {
                override fun onSuccess(
                    res: io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody<
                        IClickerAnswerResultRes>?
                ) {
                    res?.let {
                        if (!it.data.list.isNullOrEmpty()) {
                            callback.onSuccess(it.data)
                        } else {
                            callback.onSuccess(null)
                        }
                        return
                    }
                    callback.onFailure(ResponseIsEmpty)
                }

                override fun onFailure(throwable: Throwable?) {
                    AgoraLog?.e("$tag->${throwable?.let { GsonUtil.toJson(it) }}")
                }
            }))
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
    ): Call<ResponseBody<String>>

    @Headers("Content-Type: application/json")
    @PUT("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/{selectorId}/users/{userUuid}")
    fun submitAnswers(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("selectorId") selectorId: String,
        @Path("userUuid") userUuid: String,
        @Body body: IClickerSubmitAnswerBody
    ): Call<ResponseBody<String>>

    /**
     * end answer question
     */
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("edu/apps/{appId}/v2/rooms/{roomUuid}/widgets/popupQuizs/{selectorId}/stop")
    fun end(
        @Path("appId") appId: String,
        @Path("roomUuid") roomUuid: String,
        @Path("selectorId") selectorId: String
    ): Call<ResponseBody<String>>

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
    ): Call<io.agora.agoraeducore.core.internal.edu.common.bean.ResponseBody<IClickerAnswerResultRes>>
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