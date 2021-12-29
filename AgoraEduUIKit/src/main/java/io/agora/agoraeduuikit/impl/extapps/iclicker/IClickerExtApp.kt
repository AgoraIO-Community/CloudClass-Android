package io.agora.agoraeduuikit.impl.extapps.iclicker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.children
import androidx.recyclerview.widget.*
import com.google.gson.Gson
import io.agora.agoraeducore.core.context.AgoraEduContextUserInfo
import io.agora.agoraeducore.core.context.EduContextPool
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppBase
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppLaunchState.Running
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppLaunchState.Stopped
import io.agora.agoraeducore.extensions.extapp.AgoraExtAppUserRole
import io.agora.agoraeducore.extensions.extapp.TimeUtil
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetDefaultId
import io.agora.agoraeducore.extensions.widgets.AgoraWidgetMessageObserver
import io.agora.agoraeduuikit.R
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerAnswerState.end
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerAnswerState.start
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.DELETED
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_ANSWER
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_END_TIME
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_ITEMS
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_START_TIME
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STATE
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENT
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENTNAMES
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENTS
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionPacket
import io.agora.agoraeduuikit.impl.whiteboard.bean.AgoraBoardInteractionSignal
import java.util.*

class IClickerExtApp : AgoraExtAppBase() {
    private val tag = "IClickerExtApp"

    private var mContainer: RelativeLayout? = null
    private lateinit var mLayout: View
    private lateinit var studentLayout: LinearLayout
    private lateinit var studentAnswerResultLayout: LinearLayout
    private lateinit var mSubmitBtn: TextView
    private lateinit var mAnswersGridView: GridView
    private lateinit var mAnswerAdapter: AnswerAdapter
    private lateinit var mTimerText: TextView
    private var mTimer: Timer? = null

    private var mAppLoaded = false
    private var mPendingPropertyUpdate = false
    private var mPendingProperties: Map<String, Any?>? = null
    private var mPendingCause: Map<String, Any?>? = null

    private var mSubmitted = false
    private var mStartTime: Long = 0
    private var mTickCount: Long = 0

    // teacher
    private val defaultAnswerCount = 4
    private lateinit var answerItemMax: MutableList<String>
    private lateinit var close: AppCompatImageView
    private lateinit var teacherLayout: FrameLayout
    private lateinit var presetLayout: LinearLayout
    private lateinit var presetAnswersGridView: GridView
    private lateinit var plus: AppCompatImageView
    private lateinit var startAnswer: AppCompatTextView
    private lateinit var surplus: AppCompatImageView
    private lateinit var answeringLayout: LinearLayout
    private lateinit var respondentsTextView: AppCompatTextView
    private lateinit var accuracyTextView: AppCompatTextView
    private lateinit var correctAnswersTextView: AppCompatTextView
    private lateinit var studentAnswersRecyclerView: RecyclerView
    private lateinit var actionAnswer: AppCompatTextView
    private lateinit var presetAnswerAdapter: AnswerAdapter
    private lateinit var curAnswerState: String
    private lateinit var correctAnswers: List<String>
    private var studentNames: MutableList<*>? = null
    private var students: MutableList<*>? = null
    private val replies = mutableMapOf<String, ReplyItem>()
    private val whiteBoardObserver = object : AgoraWidgetMessageObserver {
        override fun onMessageReceived(msg: String, id: String) {
            val packet = Gson().fromJson(msg, AgoraBoardInteractionPacket::class.java)
            when (packet.signal) {
                AgoraBoardInteractionSignal.BoardGrantDataChanged -> {
                    eduContextPool?.userContext()?.getLocalUserInfo()?.userUuid?.let { userUuid ->
                        val granted = (packet.body as? ArrayList<String>)?.contains(userUuid)
                            ?: false
                        setDraggable(granted)
                    }
                }
            }
        }
    }
    private class AnswerAdapter(var context: Context, var dataList: MutableList<String>) : BaseAdapter() {
        private val checkedSets = mutableSetOf<Int>()
        private var onCheckChangedListener: OnCheckChangedListener? = null
        private var isEnabled = true

        inner class ViewHolder {
            lateinit var textView: CheckedTextView
        }

        interface OnCheckChangedListener {
            fun onChanged(isChecked: Boolean)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view: View?
            val viewHolder: ViewHolder?

            if (convertView == null) {
                viewHolder = ViewHolder()
                view = LayoutInflater.from(context).inflate(R.layout.iclicker_answer_preset_item, parent, false)
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
            } else {
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

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

    private lateinit var studentAnswersAdapter: StudentAnswersAdapter
    private val listUpdateCallback = object : ListUpdateCallback {
        override fun onInserted(position: Int, count: Int) {
            studentAnswersRecyclerView.post {
                studentAnswersAdapter.notifyItemRangeInserted(position, count)
            }
        }

        override fun onRemoved(position: Int, count: Int) {
            studentAnswersRecyclerView.post {
                studentAnswersAdapter.notifyItemRangeRemoved(position, count)
            }
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            studentAnswersRecyclerView.post {
                studentAnswersAdapter.notifyItemMoved(fromPosition, toPosition)
            }
        }

        override fun onChanged(position: Int, count: Int, payload: Any?) {
            (studentAnswersRecyclerView.findViewHolderForAdapterPosition(position) as? StudentAnswerHolder)?.let { holder ->
                payload?.let { payload ->
                    if (payload is Pair<*, *>) {
                        (payload.second as? ReplyItem)?.let { item ->
                            holder.bind(item)
                        }
                    }
                }
            }
        }
    }
    private val replyItemMatcher = StudentAnswerItemMatcher()
    private val differ = AsyncListDiffer(
            listUpdateCallback, AsyncDifferConfig.Builder(replyItemMatcher).build())

    private inner class StudentAnswersAdapter(var correctAnswers: MutableList<String>) :
            RecyclerView.Adapter<StudentAnswerHolder>() {

        fun notifyCorrectAnswer(data: MutableList<String>) {
            this.correctAnswers.clear()
            this.correctAnswers.addAll(data)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentAnswerHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.iclicker_answer_list_item,
                    parent, false)
            return StudentAnswerHolder(view, correctAnswers)
        }

        override fun onBindViewHolder(holder: StudentAnswerHolder, position: Int) {
            holder.bind(differ.currentList[holder.absoluteAdapterPosition])
        }

        override fun getItemCount(): Int {
            return differ.currentList.size
        }
    }

    private class StudentAnswerItemMatcher : DiffUtil.ItemCallback<ReplyItem>() {
        override fun areItemsTheSame(oldItem: ReplyItem, newItem: ReplyItem): Boolean {
            return (oldItem.answer.contentEquals(newItem.answer) && oldItem.studentName == newItem.studentName)
        }

        override fun areContentsTheSame(oldItem: ReplyItem, newItem: ReplyItem): Boolean {
            return (oldItem.startTime == newItem.startTime
                    && oldItem.replyTime == newItem.replyTime
                    && oldItem.answer.contentEquals(newItem.answer)
                    && oldItem.studentName == newItem.studentName)
        }

        override fun getChangePayload(oldItem: ReplyItem, newItem: ReplyItem): Any {
            return Pair(oldItem, newItem)
        }
    }

    @SuppressLint("SimpleDateFormat")
    private class StudentAnswerHolder(view: View, val correctAnswers: MutableList<String>) : RecyclerView.ViewHolder(view) {
        private val name: AppCompatTextView = view.findViewById(R.id.name)
        private val time: AppCompatTextView = view.findViewById(R.id.time)
        private val answers: AppCompatTextView = view.findViewById(R.id.answers)

        fun bind(item: ReplyItem) {
            itemView.post {
                name.text = item.studentName
                val curAnswerList = item.answer.sorted().toMutableList()
                val color: Int = if (curAnswerList == correctAnswers) {
                    itemView.context.resources.getColor(R.color.iclicker_answer_list_item_correct)
                } else {
                    itemView.context.resources.getColor(R.color.iclicker_answer_list_item_error)
                }
                answers.setTextColor(color)
                val correctAnswerString = if (curAnswerList.isNullOrEmpty()) "" else
                    curAnswerList.reduce { acc, s -> acc + s }
                answers.text = correctAnswerString
                if (item.startTime.toLong() > 0 && item.replyTime.toLong() > item.startTime.toLong()) {
                    val duration = item.replyTime.toLong() - item.startTime.toLong()
                    time.text = TimeUtil.stringForTimeHMS(duration, "%02d:%02d:%02d")
                }
            }
        }
    }

    @Synchronized
    override fun onExtAppLoaded(context: Context, parent: RelativeLayout, view: View, eduContextPool: EduContextPool?) {
        super.onExtAppLoaded(context, parent, view, eduContextPool)
        Log.d(tag, "onExtAppLoaded, appId=$identifier")
        setDraggable(true)

        mAppLoaded = true
        mContainer = parent
        mContainer!!.post {
            if (mPendingPropertyUpdate) {
                val properties = if (mPendingProperties != null) mPendingProperties.toString() else ""
                val cause = if (mPendingCause != null) mPendingCause.toString() else ""
                Log.d(tag, "update pending property update, $properties, $cause")
                parseProperties(mPendingProperties!!)
                mPendingPropertyUpdate = false
            }
        }
        // try to register teacher func.
        observeUserList()
        eduContextPool?.widgetContext()?.addWidgetMessageObserver(whiteBoardObserver, AgoraWidgetDefaultId.WhiteBoard.id)
    }

    @Synchronized
    override fun onPropertyUpdated(properties: MutableMap<String, Any?>?, cause: MutableMap<String, Any?>?) {
        super.onPropertyUpdated(properties, cause)
        if (!mAppLoaded) {
            Log.d(tag, "onPropertyUpdated, " + identifier +
                    ", request to update property when app is not loaded")
            mPendingPropertyUpdate = true
            mPendingProperties = properties
            mPendingCause = cause
        } else {
            Log.d(tag, "onPropertyUpdated, " + identifier +
                    ", request to update property when app is already loaded")
            parseProperties(properties!!)
        }
    }

    @Synchronized
    private fun parseProperties(properties: Map<String, Any?>) {
        val tmp = properties[PROPERTIES_KEY_STATE].toString()
        if (mSubmitted && tmp == end && curAnswerState == start) {
            mSubmitted = false
        } else if (mSubmitted && tmp == start) {
            return
        }
        curAnswerState = tmp
        when (curAnswerState) {
            start -> {
                switchTimer(properties, true)
                handleAnswering(properties)
            }
            end -> {
                switchTimer(properties, false)
                handleAnswerEnd(properties)
            }
            else -> {
                switchTimer(properties, false)
                handleIdle()
            }
        }
    }

    private fun handleAnswering(properties: Map<String, Any?>) {
        val isTeacher = extAppContext?.localUserInfo?.userRole == AgoraExtAppUserRole.TEACHER
        if (isTeacher) {
            refreshGetAnswerResult(properties)
            showAnswerResult(replies, correctAnswers.toTypedArray(), students, studentNames, false)
        } else {
            val answers = (properties[PROPERTIES_KEY_ITEMS] as ArrayList<*>).filterIsInstance<String>()
            Log.i(tag, "IClicker started: start time:" + mStartTime +
                    ", answers:" + answers)
            var myAnswers: Array<String>? = null
            val student = properties[PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid].toString()
            if (student != DELETED) {
                val reply = Gson().fromJson(student, ReplyItem::class.java)
                if (reply != null) {
                    myAnswers = reply.answer
                }
            }
            showAnswerItems(answers.toTypedArray(), myAnswers)
        }
    }

    private fun handleAnswerEnd(properties: Map<String, Any?>) {
        refreshGetAnswerResult(properties)
        val isTeacher = extAppContext?.localUserInfo?.userRole == AgoraExtAppUserRole.TEACHER
        if (isTeacher) {
            showAnswerResult(replies, correctAnswers.toTypedArray(), students, studentNames, true)
        } else {
            Log.i(tag, "IClicker(student) end: correct answers " + correctAnswers +
                    ", replies:" + replies)
            showStudentResult(replies, correctAnswers.toTypedArray(), students)
        }
    }

    private fun handleIdle() {
        val isTeacher = extAppContext?.localUserInfo?.userRole == AgoraExtAppUserRole.TEACHER
        if (isTeacher) {
            presetLayout.post {
                presetLayout.visibility = View.VISIBLE
                answeringLayout.visibility = View.GONE
            }
        } else {
            mAnswerAdapter.setChecked(mutableListOf())
            mAnswerAdapter.setEnabled(true)
        }
    }

    // get the latest results from property
    private fun refreshGetAnswerResult(properties: Map<String, Any?>) {
        properties[PROPERTIES_KEY_ANSWER]?.let {
            correctAnswers = (it as ArrayList<*>).filterIsInstance<String>()
        }
        studentNames = properties[PROPERTIES_KEY_STUDENTNAMES] as MutableList<*>?
        students = properties[PROPERTIES_KEY_STUDENTS] as MutableList<*>?
        replies.clear()
        students?.forEach { uuid ->
            val studentName: String = studentNames?.find { uuid.toString().startsWith(it.toString()) }.toString()
            val student = properties[PROPERTIES_KEY_STUDENT + uuid].toString()
            if (student != DELETED) {
                val reply = Gson().fromJson(student, ReplyItem::class.java)
                reply?.let {
                    reply.studentName = studentName
                    replies[uuid as String] = reply
                }
            }
        }
    }

    @Synchronized
    override fun onExtAppUnloaded() {
        mAppLoaded = false
        resetTimer()
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(context: Context): View {
        mLayout = LayoutInflater.from(context).inflate(R.layout.extapp_iclicker, null, false)

        // student
        studentLayout = mLayout.findViewById(R.id.student_Layout)
        studentAnswerResultLayout = mLayout.findViewById(R.id.studentAnswerResult_Layout)
        mTimerText = mLayout.findViewById(R.id.iclicker_timer_text)
        mSubmitBtn = mLayout.findViewById(R.id.submit_answer)
        mAnswersGridView = mLayout.findViewById(R.id.answers)

        // teacher
        close = mLayout.findViewById(R.id.close_Image)
        teacherLayout = mLayout.findViewById(R.id.teacher_Layout)
        presetLayout = mLayout.findViewById(R.id.preset_Layout)
        presetAnswersGridView = mLayout.findViewById(R.id.preset_answers)
        plus = mLayout.findViewById(R.id.plus_Image)
        startAnswer = mLayout.findViewById(R.id.start_answer)
        surplus = mLayout.findViewById(R.id.surplus_Image)
        answeringLayout = mLayout.findViewById(R.id.answering_Layout)
        respondentsTextView = mLayout.findViewById(R.id.respondents_TextView)
        accuracyTextView = mLayout.findViewById(R.id.accuracy_TextView)
        correctAnswersTextView = mLayout.findViewById(R.id.correct_answers_TextView)
        studentAnswersRecyclerView = mLayout.findViewById(R.id.answer_RecyclerView)
        actionAnswer = mLayout.findViewById(R.id.action_answer)
        readyUIByRole(context)

        return mLayout
    }

    private fun onSubmitClick() {
        submitOrModifyAnswers(!mSubmitBtn.isSelected)
        submitProperties()
    }

    private fun submitOrModifyAnswers(submit: Boolean) {
        if (submit) {
            mSubmitted = true
        }
        mSubmitBtn.isSelected = submit
        mSubmitBtn.setText(if (submit) R.string.modify_answer else R.string.submit_answer)
        mAnswerAdapter.setEnabled(!submit)
        for (view in mAnswersGridView.children) {
            view.isEnabled = !submit
        }
    }

    private fun submitProperties() {
        val myAnswers = mAnswerAdapter.getCheckedItems()
                .sorted()
                .map { ('A' + it).toString() }
                .toTypedArray()

        val replyItem = getLocalUserInfo()?.userUuid?.let {
            ReplyItem(
                    mStartTime.toString(),
                    (TimeUtil.currentTimeMillis() / 1000).toString(),
                    myAnswers
            )
        }
        updateProperties(mutableMapOf(PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid to replyItem),
                mutableMapOf("startTime" to mStartTime.toString()), null)
    }

    // show answer items for student to choose
    private fun showAnswerItems(answers: Array<String>, my: Array<String>?) {
        mAnswersGridView.post {
            studentLayout.visibility = View.VISIBLE
            studentAnswerResultLayout.visibility = View.GONE

            mAnswerAdapter.dataList = answers.toMutableList()
            my?.map { it.toCharArray()[0] }?.map { it - 'A' }?.let {
                mAnswerAdapter.setChecked(it)
                mSubmitBtn.isEnabled = true
                submitOrModifyAnswers(true)
            }
            mAnswerAdapter.notifyDataSetChanged()
        }
    }

    private fun showStudentResult(replies: MutableMap<String, ReplyItem>, correctAnswers: Array<String>,
                                  students: MutableList<*>?) {
        mLayout.post {
            mTimerText.isEnabled = false
            studentLayout.visibility = View.GONE
            studentAnswerResultLayout.visibility = View.VISIBLE

            val submits = replies.filterNot { it.value.answer.isNullOrEmpty() }.values
            val correctAnswerString = correctAnswers.sorted().reduce { acc, s -> acc + s }
            val sumCorrect = submits.filter { it.answer.sorted().reduce { acc, s -> acc + s } == correctAnswerString }.size
            val total = if (students.isNullOrEmpty()) 0 else students.size
            val respondents = submits.size.toString() + "/" + total
            val accuracy = if (sumCorrect != 0) (sumCorrect * 100 / total).toString() + "%" else "0%"

            mLayout.findViewById<TextView>(R.id.number_of_respondents).text = respondents
            mLayout.findViewById<TextView>(R.id.accuracy).text = accuracy
            mLayout.findViewById<TextView>(R.id.correct_answers).text = correctAnswerString

            val myAnswers = replies[getLocalUserInfo()?.userUuid]?.answer?.joinToString("")
            val myAnswersTextView = mLayout.findViewById<TextView>(R.id.my_answers)
            myAnswersTextView.text = myAnswers
            myAnswersTextView.isSelected = correctAnswerString == myAnswers

            reLayout()
        }
    }

    private fun reLayout() {
        val params = mLayout.layoutParams as RelativeLayout.LayoutParams
        val width = mLayout.measuredWidth
        val height = mLayout.measuredHeight
        var top = params.topMargin
        var left = params.leftMargin
        val parentWidth = mContainer!!.width
        val parentHeight = mContainer!!.height

        if (left + width > parentWidth) {
            left = parentWidth - width
        }

        if (top + height > parentHeight) {
            top = parentHeight - height
        }

        params.leftMargin = left
        params.topMargin = top
        mLayout.layoutParams = params
    }

    // show the results of answer for the teacher
    private fun showAnswerResult(replies: MutableMap<String, ReplyItem>, correctAnswers: Array<String>,
                                 students: MutableList<*>?, studentNames: MutableList<*>?, end: Boolean) {
        answeringLayout.post {
            presetLayout.visibility = View.GONE
            answeringLayout.visibility = View.VISIBLE
            mTimerText.isEnabled = false

            if (correctAnswers.isNotEmpty() && !students.isNullOrEmpty()) {
                val submits = replies.filterNot { it.value.answer.isNullOrEmpty() }.values
                val correctAnswerString = correctAnswers.sorted().reduce { acc, s -> acc + s }
                val sumCorrect = submits.filter { it.answer.sorted().reduce { acc, s -> acc + s } == correctAnswerString }.size
                val total = if (students.isNullOrEmpty()) 0 else students.size
                val respondents = String.format(mLayout.context.resources.getString(R.string.respondentsnum),
                        submits.size, total)
                val percent = if (sumCorrect != 0) (sumCorrect * 100 / total) else 0
                val accuracy = String.format(mLayout.context.resources.getString(R.string.accuracynum),
                        percent)
                respondentsTextView.text = respondents
                accuracyTextView.text = accuracy
                correctAnswersTextView.text = correctAnswerString
                actionAnswer.text = mLayout.context.resources.getString(
                        if (end) R.string.start_again else R.string.end_answer)
            }
            if (!studentNames.isNullOrEmpty()) {
                // show answer list
                studentAnswersAdapter.notifyCorrectAnswer(correctAnswers.sorted().toMutableList())
                val datas = mutableListOf<ReplyItem>()
                replies.forEach {
                    datas.add(it.value)
                }
                val tmp = datas.map { it.studentName }
                val noReplyStudentNames = studentNames.filter { !tmp.contains(it) }
                noReplyStudentNames.forEach {
                    val replyItem = ReplyItem("0", "0", arrayOf(), it.toString())
                    datas.add(replyItem)
                }
                differ.submitList(datas)
            }
        }
    }

    private fun switchTimer(properties: Map<String, Any?>, timerRunning: Boolean) {
        val startTmp = properties[PROPERTIES_KEY_START_TIME]?.toString()
        mStartTime = if (startTmp.isNullOrEmpty()) 0 else startTmp.toLong()
        if (timerRunning) {
            val tick = TimeUtil.currentTimeMillis() / 1000 - mStartTime
            if (tick >= 0) {
                mTickCount = tick
            }
            resetTimer()
            mTimer = Timer()
            mTimer?.schedule(object : TimerTask() {
                override fun run() {
                    mTickCount++
                    mTimerText.post {
                        mTimerText.text = TimeUtil.stringForTimeHMS(mTickCount, "%02d:%02d:%02d")
                    }
                }
            }, 1000, 1000)
        } else {
            val endTmp = properties[PROPERTIES_KEY_END_TIME]?.toString()
            val endTime = if (endTmp.isNullOrEmpty()) 0 else endTmp.toLong()
            mTimerText.post {
                mTimerText.text = TimeUtil.stringForTimeHMS(endTime - mStartTime, "%02d:%02d:%02d")
            }
            resetTimer()
        }
        mTimerText.post {
            mTimerText.visibility = if (timerRunning) View.VISIBLE else View.GONE
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
        val isTeacher = extAppContext?.localUserInfo?.userRole == AgoraExtAppUserRole.TEACHER
        if (!isTeacher) {
            studentLayout.visibility = View.VISIBLE
            teacherLayout.visibility = View.GONE
            mSubmitBtn.setOnClickListener { onSubmitClick() }
            mAnswerAdapter = AnswerAdapter(context, mutableListOf())
            mAnswersGridView.adapter = mAnswerAdapter
            mAnswerAdapter.setOnCheckedTextView(object : AnswerAdapter.OnCheckChangedListener {
                override fun onChanged(isChecked: Boolean) {
                    mSubmitBtn.isEnabled = isChecked
                }
            })
        } else {
            close.visibility = View.VISIBLE
            studentLayout.visibility = View.GONE
            teacherLayout.visibility = View.VISIBLE
            answerItemMax = context.resources.getStringArray(R.array.answer_item).toMutableList()
            close.setOnClickListener {
                closeSelf()
            }
            plus.setOnClickListener {
                presetAnswerAdapter.let {
                    if (it.count < answerItemMax.size) {
                        it.modifyData(answerItemMax[it.count], true)
                    }
                    if (it.count >= answerItemMax.size) {
                        plus.visibility = View.INVISIBLE
                    }
                    if (it.count > defaultAnswerCount / 2) {
                        surplus.visibility = View.VISIBLE
                    }
                }
            }
            startAnswer.setOnClickListener {
//                presetLayout.visibility = View.GONE
//                answeringLayout.visibility = View.VISIBLE
                resetTimer()
                startAnswer()
            }
            surplus.setOnClickListener {
                presetAnswerAdapter.let {
                    if (it.count > defaultAnswerCount / 2) {
                        presetAnswerAdapter.modifyData(add = false)
                    }
                    if (it.count <= defaultAnswerCount / 2) {
                        surplus.visibility = View.INVISIBLE
                    }
                    if (it.count < answerItemMax.size) {
                        plus.visibility = View.VISIBLE
                    }
                    startAnswer.isEnabled = it.getCheckedItems().isNotEmpty()
                }
            }
            actionAnswer.setOnClickListener {
                when (curAnswerState) {
                    start -> {
                        stopAnswer()
                        resetTimer()
                        actionAnswer.text = context.resources.getString(R.string.start_again)
                    }
                    end -> {
                        refreshPresetAnswer(context)
                        restartAnswer()
//                        presetLayout.visibility = View.VISIBLE
//                        answeringLayout.visibility = View.GONE
                        actionAnswer.text = context.resources.getString(R.string.end_answer)
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
        presetAnswersGridView.adapter = presetAnswerAdapter
        presetAnswerAdapter.setOnCheckedTextView(object : AnswerAdapter.OnCheckChangedListener {
            override fun onChanged(isChecked: Boolean) {
                startAnswer.isEnabled = isChecked
            }
        })
        startAnswer.isEnabled = false
    }

    // only for teacher to manager students/studentNames
    private fun observeUserList() {
        val isTeacher = extAppContext?.localUserInfo?.userRole == AgoraExtAppUserRole.TEACHER
        if (!isTeacher) {
            return
        }
//        val userHandler = object : UserHandler() {
//            override fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>) {
//                super.onUserListUpdated(list)
//                val studentInfos = list.filter { it.user.role == AgoraEduContextUserRole.Student }
//                        .map { it.user }.toMutableList()
//                upsertStudentProperty(studentInfos, false)
//            }
//        }
        // todo extAppContext will be rewrite,
//        eduContextPool?.userContext()?.addHandler(userHandler)
    }

    private fun initStudentProperty() {
        // init check students/studentNames while start answer.
        // todo call userContext3().getAllUserList()
//        eduContextPool?.userContext()?.curUserList(object : EduContextCallback<MutableList<AgoraEduContextUserInfo>> {
//            override fun onSuccess(target: MutableList<AgoraEduContextUserInfo>?) {
//                val studentInfos = target?.filter { it.role == EduContextUserRole.Student }?.toMutableList()
//                upsertStudentProperty(studentInfos, true)
//            }
//
//            override fun onFailure(error: EduContextError?) {}
//        })
    }

    private fun upsertStudentProperty(studentInfos: MutableList<AgoraEduContextUserInfo>?, running: Boolean) {
        val studentNames = mutableListOf<String>()
        val students = mutableListOf<String>()
        studentInfos?.forEach {
            studentNames.add(it.userName)
            students.add(it.userUuid)
        }
        val status = IClickerStatus(studentNames = studentNames, students = students)
        val common: MutableMap<String, Any?> = mutableMapOf(Pair(PROPERTIES_KEY_STATE, Running.value))
        updateProperties(status.convert() ?: mutableMapOf(), mutableMapOf(),
                if (running) common else null, null)
    }

    private fun closeSelf() {
        val status = IClickerStatus(state = end)
        val common: MutableMap<String, Any?> = mutableMapOf(Pair(PROPERTIES_KEY_STATE, Stopped.value))
        updateProperties(status.convert() ?: mutableMapOf(), mutableMapOf(), common, null)
    }

    private fun readyAnsweringUI(context: Context) {
        val lm = object : LinearLayoutManager(context, HORIZONTAL, false) {
            override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        studentAnswersRecyclerView.layoutManager = lm
        studentAnswersAdapter = StudentAnswersAdapter(mutableListOf())
        studentAnswersRecyclerView.adapter = studentAnswersAdapter
    }

    private fun startAnswer() {
        val answer = presetAnswerAdapter.getCheckedItems()
                .sorted()
                .map { answerItemMax[it] }
                .toMutableList()
        val items = presetAnswerAdapter.dataList
        val startTime = TimeUtil.currentTimeMillis() / 1000
        val status = IClickerStatus(answer = answer, canChange = true, items = items,
                startTime = startTime.toString(), state = start)
        val common: MutableMap<String, Any?> = mutableMapOf(Pair(PROPERTIES_KEY_STATE, Running.value))
        updateProperties(status.convert() ?: mutableMapOf(), mutableMapOf(), common, null)
        // when start answer, initStudentProperty
        initStudentProperty()
    }

    private fun stopAnswer() {
        val endTime = TimeUtil.currentTimeMillis() / 1000
        val status = IClickerStatus(endTime = endTime.toString(), state = end)
        updateProperties(status.convert() ?: mutableMapOf(), mutableMapOf(), null, null)
    }

    // start again
    private fun restartAnswer() {
        students?.let {
            val delKeys = mutableListOf<String>()
            it.forEach { uuid ->
                delKeys.add(PROPERTIES_KEY_STUDENT.plus(uuid))
            }
            deleteProperties(delKeys, mutableMapOf(), null)
            val status = IClickerStatus(answer = mutableListOf(), endTime = "", items = mutableListOf(),
                    startTime = "", state = "", studentNames = mutableListOf(), students = mutableListOf())
            val common: MutableMap<String, Any?> = mutableMapOf(Pair(PROPERTIES_KEY_STATE, null))
            updateProperties(status.convert() ?: mutableMapOf(), mutableMapOf(), common, null)
        }
    }
}
