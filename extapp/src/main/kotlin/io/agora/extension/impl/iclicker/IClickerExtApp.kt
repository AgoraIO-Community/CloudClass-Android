package io.agora.extension.impl.iclicker

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.view.children
import com.google.gson.Gson
import io.agora.extension.AgoraExtAppBase
import io.agora.extension.R
import io.agora.extension.TimeUtil
import java.util.*
import kotlin.collections.ArrayList

class IClickerExtApp : AgoraExtAppBase() {
    private var mContainer: RelativeLayout? = null
    private lateinit var mLayout: View
    private lateinit var mSubmitBtn: TextView
    private lateinit var mAnswersGridView: GridView
    private lateinit var mAnswerAdapter: AnswerAdapter
    private lateinit var mTimerText: TextView
    private var mTimer: Timer = Timer()

    private var mAppLoaded = false
    private var mPendingPropertyUpdate = false
    private var mPendingProperties: Map<String, Any?>? = null
    private var mPendingCause: Map<String, Any?>? = null

    private var mState = ""
    private var mStartTime: Long = 0
    private var mTickCount: Long = 0

    class AnswerAdapter (var context: Context, private var dataSet: Array<String>) : BaseAdapter(){
        private val checkedArrays = mutableSetOf<Int>()
        private var onCheckChangedListener: OnCheckChangedListener? = null
        private var isEnabled = true

        inner class ViewHolder {
            lateinit var textView : CheckedTextView
        }

        interface OnCheckChangedListener {
            fun onChanged(isChecked: Boolean)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view:View?
            val viewHolder: ViewHolder?

            if(convertView == null){
                viewHolder = ViewHolder()
                view = LayoutInflater.from(context).inflate(R.layout.iclicker_answer_item, parent, false)
                viewHolder.textView = view.findViewById(R.id.iclicker_answer_item_text)
                viewHolder.textView.setOnClickListener {
                    val contain = isCheckedPosition(position)
                    viewHolder.textView.isChecked = !contain
                    if (contain) {
                        checkedArrays.remove(position)
                    } else {
                        checkedArrays.add(position)
                    }
                    onCheckChangedListener?.onChanged(isChecked())
                }
                view.tag = viewHolder
            }else{
                view = convertView
                viewHolder = view.tag as ViewHolder
            }

            viewHolder.textView.text = dataSet[position]
            viewHolder.textView.isChecked = isCheckedPosition(position)
            viewHolder.textView.isEnabled = isEnabled

             return view!!
        }

        override fun getItem(position: Int) = dataSet[position]

        override fun getItemId(position: Int) = position.toLong()

        override fun getCount() = dataSet.size

        fun setData(data: Array<String>) {
            dataSet = data
        }

        fun setChecked(selected: List<Int>) {
            checkedArrays.clear()
            checkedArrays.addAll(selected)
        }

        fun setEnabled(enabled: Boolean) {
            isEnabled = enabled
        }

        private fun isCheckedPosition(position: Int): Boolean {
            if (checkedArrays.isEmpty()) {
                return false
            }
            return checkedArrays.indexOf(position) != -1
        }

        private fun isChecked() = checkedArrays.isNotEmpty()

        fun getCheckedItems() = checkedArrays

        fun setOnCheckedTextView(listener: OnCheckChangedListener) {
            onCheckChangedListener = listener
        }
    }

    @Synchronized
    override fun onExtAppLoaded(context: Context, parent: RelativeLayout) {
        Log.d(TAG, "onExtAppLoaded, appId=$identifier")
        mAppLoaded = true
        mContainer = parent
        mContainer!!.post {
            setLayoutParams()

            if (mPendingPropertyUpdate) {
                val properties = if (mPendingProperties != null) mPendingProperties.toString() else ""
                val cause = if (mPendingCause != null) mPendingCause.toString() else ""
                Log.d(TAG, "update pending property update, $properties, $cause")
                parseProperties(mPendingProperties!!)
                mPendingPropertyUpdate = false
            }
        }

    }

    @Synchronized
    override fun onPropertyUpdated(properties: MutableMap<String, Any?>?, cause: MutableMap<String, Any?>?) {
        if (!mAppLoaded) {
            Log.d(TAG, "onPropertyUpdated, " + identifier +
                    ", request to update property when app is not loaded")
            mPendingPropertyUpdate = true
            mPendingProperties = properties
            mPendingCause = cause
        } else {
            Log.d(TAG, "onPropertyUpdated, " + identifier +
                    ", request to update property when app is already loaded")
            parseProperties(properties!!)
        }
    }

    @Synchronized
    private fun parseProperties(properties: Map<String, Any?>) {
        val state = properties[PROPERTIES_KEY_STATE].toString()
        if (state == STATE_START && mState == STATE_SUBMITTED) {
            return
        }
        mState = state
        if (mState == STATE_START) {
            mStartTime = properties[PROPERTIES_KEY_START_TIME].toString().toLong()
            val answers = (properties[PROPERTIES_KEY_ITEMS] as ArrayList<*>).filterIsInstance<String>()
            Log.i(TAG, "IClicker started: start time:" + mStartTime +
                    ", answers:" + answers)

            val  tick = TimeUtil.currentTimeMillis() / 1000 - mStartTime
            if (tick > 0) {
                mTickCount = tick
            }

            var myAnswers: Array<String>? = null
            val student = properties[PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid].toString()
            if (student != DELETED) {
                val reply = Gson().fromJson(student, ReplyItem::class.java)
                if (reply != null) {
                    myAnswers = reply.answer
                }
            }

            showAnswers(answers.toTypedArray(), myAnswers)
        } else if (mState == STATE_END) {
            val startTime = properties[PROPERTIES_KEY_START_TIME].toString().toLong()
            val endTime = properties[PROPERTIES_KEY_END_TIME].toString().toLong()
            val correctAnswers = (properties[PROPERTIES_KEY_ANSWER] as ArrayList<*>).filterIsInstance<String>()
            val students = properties[PROPERTIES_KEY_STUDENTS] as ArrayList<*>
            val replies = mutableMapOf<String, ReplyItem>()
            mTimerText.post {
                mTimerText.text = TimeUtil.stringForTimeHMS(endTime - startTime, "%02d:%02d:%02d")
            }
            students.forEach { uuid ->
                val student = properties[PROPERTIES_KEY_STUDENT + uuid].toString()
                if (student != DELETED) {
                    val reply = Gson().fromJson(student, ReplyItem::class.java)
                    if (reply != null) {
                        replies[uuid as String] = reply
                    }
                }

            }

            Log.i(TAG, "IClicker end: correct answers " + correctAnswers +
                    ", replies:" + replies)

            showResult(replies, correctAnswers.toTypedArray(), students.size)
        }
    }

    @Synchronized
    override fun onExtAppUnloaded() {
        mAppLoaded = false
        mTimer.cancel()
    }

    @SuppressLint("InflateParams")
    override fun onCreateView(content: Context): View {
        mLayout = LayoutInflater.from(content).inflate(R.layout.extapp_iclicker, null, false)

        mTimerText = mLayout.findViewById(R.id.iclicker_timer_text)

        mSubmitBtn = mLayout.findViewById(R.id.submit_answer)
        mSubmitBtn.setOnClickListener { onSubmitClick() }

        mTimer.schedule(object: TimerTask() {
            override fun run() {
                mTickCount++
                mTimerText.post {
                    mTimerText.text = TimeUtil.stringForTimeHMS(mTickCount, "%02d:%02d:%02d")
                }
            }
        }, 1000, 1000)

        mAnswerAdapter = AnswerAdapter(content, arrayOf())
        mAnswersGridView = mLayout.findViewById(R.id.answers)
        mAnswersGridView.adapter = mAnswerAdapter

        mAnswerAdapter.setOnCheckedTextView(object: AnswerAdapter.OnCheckChangedListener {
            override fun onChanged(isChecked: Boolean) {
                mSubmitBtn.isEnabled = isChecked
            }
        })

        return mLayout
    }

    private fun setLayoutParams() {
        mLayout.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    Log.d(TAG, "onExtAppLoaded, layout, $this")
                    if (mLayout.width > 0 && mLayout.height > 0) {
                        if (mLayout.viewTreeObserver.isAlive) {
                            mLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }

                        // The content layout will be default in the center of parent container view
                        val width = mContainer!!.width
                        val height = mContainer!!.height
                        val params = mLayout.layoutParams as RelativeLayout.LayoutParams
                        params.leftMargin = (width - mLayout.width) / 2
                        params.topMargin = (height - mLayout.height) / 2
                        mLayout.layoutParams = params
                    }
                }
            })
    }

    private fun onSubmitClick() {
        if (mState == STATE_START) {
            submitAnswers()
        } else if (mState == STATE_SUBMITTED){
            modifyAnswers()
        }
    }

    private fun submitAnswers(doSubmit: Boolean = true) {
        mState = STATE_SUBMITTED
        mSubmitBtn.isSelected = true
        mSubmitBtn.setText(R.string.modify_answer)
        mAnswerAdapter.setEnabled(false)
        for (view in mAnswersGridView.children) {
            view.isEnabled = false
        }
        if (doSubmit) {
            submitProperties()
        }
    }

    private fun modifyAnswers() {
        mState = STATE_START
        mSubmitBtn.isSelected = false
        mSubmitBtn.setText(R.string.submit_answer)
        mAnswerAdapter.setEnabled(true)
        for (view in mAnswersGridView.children) {
            view.isEnabled = true
        }
    }

    private fun submitProperties() {
        val myAnswers = mAnswerAdapter.getCheckedItems()
            .sorted()
            .map { ('A' + it).toString() }
            .toTypedArray()

        val replyItem = getLocalUserInfo()?.userUuid?.let {
            ReplyItem(
                (TimeUtil.currentTimeMillis() / 1000).toString(),
                myAnswers
            )
        }
        updateProperties(mutableMapOf(PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid to replyItem),
            mutableMapOf(), null)
    }

    private fun showAnswers(answers: Array<String>, my: Array<String>?) {
        mAnswersGridView.post {
            mAnswerAdapter.setData(answers)
            my?.map { it.toCharArray()[0] }?.map { it - 'A' }?.let {
                mAnswerAdapter.setChecked(it)
                mSubmitBtn.isEnabled = true
                submitAnswers(false)
            }
            mAnswerAdapter.notifyDataSetChanged()
        }
    }

    private fun showResult(replies: MutableMap<String, ReplyItem>, right: Array<String>, total: Int) {
        mTimer.cancel()

        val content: FrameLayout = mLayout.findViewById(R.id.iclicker_content)
        content.post {
            mTimerText.isEnabled = false


            content.removeAllViews()
            val result = LayoutInflater.from(mLayout.context).inflate(R.layout.view_iclicker_result ,
                content , false)
            content.addView(result)

            val submits = replies.filterNot { it.value.answer.isNullOrEmpty() }.values
            val rightString = right.sorted().reduce { acc, s -> acc + s }
            val sumCorrect = submits.filter { it.answer.sorted().reduce { acc, s -> acc + s } == rightString }.size
            val respondents = submits.size.toString() + "/" + total
            val accuracy = if (sumCorrect != 0) (sumCorrect * 100 / submits.size).toString() + "%" else "0%"

            result.findViewById<TextView>(R.id.number_of_respondents).text = respondents
            result.findViewById<TextView>(R.id.accuracy).text = accuracy
            result.findViewById<TextView>(R.id.correct_answers).text = rightString

            val myAnswers = replies[getLocalUserInfo()?.userUuid]?.answer?.joinToString("")
            val myAnswersTextView = result.findViewById<TextView>(R.id.my_answers)
            myAnswersTextView.text = myAnswers
            myAnswersTextView.isSelected = rightString == myAnswers

            setLayoutParams()
        }
    }

    companion object {
        private const val TAG = "iClickerExtApp"
        private const val PROPERTIES_KEY_STATE = "state"
        private const val PROPERTIES_KEY_START_TIME = "startTime"
        private const val PROPERTIES_KEY_END_TIME = "endTime"
        private const val PROPERTIES_KEY_ITEMS = "items"
        private const val PROPERTIES_KEY_ANSWER = "answer"
        private const val PROPERTIES_KEY_STUDENT = "student"
        private const val PROPERTIES_KEY_STUDENTS = "students"
//        private const val PROPERTIES_KEY_REPLY_TIME = "replyTime"
        private const val DELETED = "deleted"
        private const val STATE_START = "start"
        private const val STATE_SUBMITTED = "submitted"
        private const val STATE_END = "end"
    }
}