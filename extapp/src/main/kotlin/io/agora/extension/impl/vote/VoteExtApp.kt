package io.agora.extension.impl.vote

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.agora.extension.AgoraExtAppBase
import io.agora.extension.R
import io.agora.extension.TimeUtil
import kotlin.math.abs

class VoteExtApp : AgoraExtAppBase() {
    private var mContainer: RelativeLayout? = null
    private lateinit var mLayout: View
    private lateinit var mVoteBtn: Button
    private lateinit var mChoicesRecyclerView: RecyclerView
    private lateinit var mChoiceAdapter: ChoiceAdapter
    private lateinit var mChoices: Array<String>

    private var mAppLoaded = false
    private var mPendingPropertyUpdate = false
    private var mPendingProperties: Map<String, Any?>? = null
    private var mPendingCause: Map<String, Any?>? = null

    private var mState = ""
    private var mStartTime: Long = 0

    private var mLastPointerId = -1
    private var mLastTouchX = -1
    private var mLastTouchY = -2
    private var mTouched = false

    class ChoiceAdapter(private var dataSet: Array<String>, private var multiChoice: Boolean) :
        RecyclerView.Adapter<ChoiceAdapter.ViewHolder>() {
        val checkedArrays = mutableSetOf<String>()
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
                .inflate(R.layout.vote_choice_item, viewGroup, false)

            val viewHolder = ViewHolder(view)
            if (multiChoice) {
                viewHolder.checkView.setBackgroundResource(R.drawable.check_bg)
            }
            return viewHolder
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            viewHolder.textView.text = dataSet[position]
            viewHolder.checkView.isSelected = isCheckedPosition(dataSet[position])
            viewHolder.textView.isSelected = isCheckedPosition(dataSet[position])
            viewHolder.itemView.setOnClickListener {
                val contain = isCheckedPosition(dataSet[position])
                viewHolder.textView.isSelected = !contain
                viewHolder.checkView.isSelected = !contain
                if (isChecked() && !multiChoice) {
                    notifyItemChanged(dataSet.indexOf(checkedArrays.elementAt(0)))
                    checkedArrays.clear()
                }

                if (contain) {
                    checkedArrays.remove(dataSet[position])
                } else {
                    checkedArrays.add(dataSet[position])
                }
                onCheckChangedListener?.onChanged(isChecked())
            }
        }

        override fun getItemCount() = dataSet.size

        private fun isCheckedPosition(choice: String): Boolean {
            if (checkedArrays.isEmpty()) {
                return false
            }
            return checkedArrays.indexOf(choice) != -1
        }

        private fun isChecked() = checkedArrays.isNotEmpty()

        fun getCheckedItems() = checkedArrays

        fun setOnCheckedTextView(listener: OnCheckChangedListener) {
            onCheckChangedListener = listener
        }

        fun setData(data: Array<String>, multi: Boolean) {
            dataSet = data
            multiChoice = multi
            notifyItemRangeChanged(0, data.size)
        }
    }

    class ResultAdapter(private var dataSet: MutableMap<Int, ResultItem>) :
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
                .inflate(R.layout.vote_result_item, viewGroup, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
            dataSet[position]?.let {
                viewHolder.choiceView.text = it.choice
                val proportion = "(${it.count}) ${it.proportion}%"
                viewHolder.proportionView.text = proportion
                viewHolder.progressBarView.progress = it.proportion
            }
        }

        override fun getItemCount() = dataSet.size
    }

    @Synchronized
    override fun onExtAppLoaded(context: Context, parent: RelativeLayout) {
        Log.d(TAG, "onExtAppLoaded, appId=$identifier")

        mAppLoaded = true
        mContainer = parent
        mContainer!!.post {
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
        if (mState != STATE_SUBMITTED) {
            mState = state
        }

        val parseReplies = {
            val choices = (properties[PROPERTIES_KEY_ITEMS] as ArrayList<*>).filterIsInstance<String>()
            val students = properties[PROPERTIES_KEY_STUDENTS] as ArrayList<*>
            val replies = ArrayList<ReplyItem>()
            students.forEach { uuid ->
                val student = properties[PROPERTIES_KEY_STUDENT + uuid].toString()
                if (student != DELETED) {
                    val reply = Gson().fromJson(student, ReplyItem::class.java)
                    if (reply != null) {
                        replies.add(reply)
                    }
                }
            }

            Log.i(TAG, "Vote end: replays $replies")

            showResult(choices.toTypedArray(), replies.toTypedArray())
        }

        if (mState == STATE_START) {
            mStartTime = properties[PROPERTIES_KEY_START_TIME].toString().toLong()
            val title = properties[PROPERTIES_KEY_TITLE].toString()
            val multiChoice = properties[PROPERTIES_KEY_MULTI_CHOICE].toString().toBoolean()
            mChoices = (properties[PROPERTIES_KEY_ITEMS] as ArrayList<*>).filterIsInstance<String>().toTypedArray()
            Log.i(TAG, "Vote started: start time:" + mStartTime + ", title:" + title +
                    ", multiChoice:" + multiChoice + ", choices:" + mChoices)

            showVote(title, multiChoice)
            val myVote = properties[PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid]
            if (myVote != null && myVote != DELETED) {
                parseReplies()
            }
        } else if (mState == STATE_SUBMITTED || mState == STATE_END) {
            parseReplies()
        }
    }

    @Synchronized
    override fun onExtAppUnloaded() {
        mAppLoaded = false
    }

    @SuppressLint( "ClickableViewAccessibility", "InflateParams")
    override fun onCreateView(content: Context): View {
        mLayout = LayoutInflater.from(content).inflate(R.layout.extapp_vote, null, false)
        mLayout.isClickable = true
        mLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Only detect the touch events of the first pointer
                    if (mLastPointerId != -1) {
                        if (mLastPointerId != event.getPointerId(0)) {
                            // Current touching pointer is not the pointer of current touch event,
                            // this event will be ignored.
                            return@setOnTouchListener false
                        }
                    } else {
                        mLastPointerId = event.getPointerId(0)
                    }
                    mLastTouchX = event.rawX.toInt()
                    mLastTouchY = event.rawY.toInt()
                    mTouched = true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!mTouched || event.getPointerId(0) != mLastPointerId) {
                        return@setOnTouchListener false
                    }
                    if (!coordinateInRange(event.rawX.toInt(), event.rawY.toInt())) {
                        return@setOnTouchListener false
                    }
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    reLayout(x, y)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    Log.d(TAG, "on layout touch up or canceled")
                    mLastPointerId = -1
                    mLastTouchX = -1
                    mLastTouchY = -1
                    mTouched = false
                }
            }
            false
        }


        mVoteBtn = mLayout.findViewById(R.id.vote_btn)
        mVoteBtn.setOnClickListener { onVoteClick() }

        mChoiceAdapter = ChoiceAdapter(arrayOf(), false)
        mChoicesRecyclerView = mLayout.findViewById(R.id.vote_choices)
        mChoicesRecyclerView.adapter = mChoiceAdapter
        mChoiceAdapter.setOnCheckedTextView(object: ChoiceAdapter.OnCheckChangedListener {
            override fun onChanged(isChecked: Boolean) {
                mVoteBtn.isEnabled = isChecked
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

    private fun coordinateInRange(x: Int, y: Int): Boolean {
        val location = IntArray(2)
        mLayout.getLocationOnScreen(location)
        val layoutX = location[0]
        val layoutY = location[1]
        val layoutW = mLayout.width
        val layoutH = mLayout.height
        return layoutX <= x && x <= layoutX + layoutW &&
                layoutY <= y && y <= layoutY + layoutH
    }

    private fun reLayout(x: Int, y: Int) {
        if (mContainer == null) {
            return
        }
        if (mLayout.parent !== mContainer) {
            return
        }
        var diffX: Int = x - mLastTouchX
        var diffY: Int = y - mLastTouchY
        if (abs(diffX) < MIN_MOVE_DISTANCE_X) {
            diffX = 0
        }
        if (abs(diffY) < MIN_MOVE_DISTANCE_Y) {
            diffY = 0
        }
        val params = mLayout.layoutParams as RelativeLayout.LayoutParams
        val width = mLayout.width
        val height = mLayout.height
        var top = params.topMargin
        var left = params.leftMargin
        val parentWidth = mContainer!!.width
        val parentHeight = mContainer!!.height
        if (diffX < 0) {
            if (left + diffX < 0) {
                left = 0
            } else {
                left += diffX
            }
        } else {
            if (left + width + diffX > parentWidth) {
                left = parentWidth - width
            } else {
                left += diffX
            }
        }
        if (diffY < 0) {
            if (top + diffY < 0) {
                top = 0
            } else {
                top += diffY
            }
        } else {
            if (top + height + diffY > parentHeight) {
                top = parentHeight - height
            } else {
                top += diffY
            }
        }
        params.leftMargin = left
        params.topMargin = top
        mLayout.layoutParams = params
        mLastTouchX += diffX
        mLastTouchY += diffY
    }

    private fun onVoteClick() {
        submitVote()
        mState = STATE_SUBMITTED
        showResult(mChoices, null)
    }

    private fun submitVote() {
        val replyItem = getLocalUserInfo()?.userUuid?.let {
            ReplyItem(
                TimeUtil.currentTimeMillis().toString(),
                mChoiceAdapter.getCheckedItems().toTypedArray()
            )
        }
        updateProperties(mutableMapOf(PROPERTIES_KEY_STUDENT + getLocalUserInfo()?.userUuid to replyItem),
            mutableMapOf(), null)
    }

    private fun showVote(title: String, multiChoice: Boolean) {
        mLayout.post {
            if (multiChoice) {
                mLayout.findViewById<TextView>(R.id.vote_selection_mode).text = mLayout.resources.getString(R.string.multi)
            } else {
                mLayout.findViewById<TextView>(R.id.vote_selection_mode).text =  mLayout.resources.getString(R.string.single)
            }
            mLayout.findViewById<TextView>(R.id.vote_title).text = title

            mChoiceAdapter.setData(mChoices, multiChoice)
            setLayoutParams()
        }
    }

    private fun showResult(choices: Array<String>, replies: Array<ReplyItem>?) {
        mLayout.post {
            mLayout.findViewById<Button>(R.id.vote_btn).visibility = View.INVISIBLE

            val results = mutableMapOf<Int, ResultItem>()
            choices.forEachIndexed { index, s -> results[index] = ResultItem(s, 0,0) }

            var count = 0
            replies?.forEach {
                if (!it.answer.isNullOrEmpty()) {
                    count++
                }

                it.answer.forEach { choice ->
                    val index = choices.indexOf(choice)
                    if (index != -1) {
                        results[index]!!.count++
                    }
                }
            }

            results.forEach {
                if (it.value.count != 0) {
                    it.value.proportion = it.value.count * 100 / count
                }
            }
            mChoicesRecyclerView.adapter = ResultAdapter(results)
        }
    }

    companion object {
        private const val TAG = "VoteExtApp"
        private const val PROPERTIES_KEY_STATE = "state"
        private const val PROPERTIES_KEY_MULTI_CHOICE = "mulChoice"
        private const val PROPERTIES_KEY_START_TIME = "startTime"
        private const val PROPERTIES_KEY_TITLE = "title"
        private const val PROPERTIES_KEY_ITEMS = "items"
        private const val PROPERTIES_KEY_STUDENT = "student"
        private const val PROPERTIES_KEY_STUDENTS = "students"
//        private const val PROPERTIES_KEY_REPLY_TIME = "replyTime"
        private const val DELETED = "deleted"
        private const val STATE_START = "start"
        private const val STATE_SUBMITTED = "submitted"
        private const val STATE_END = "end"

        private const val MIN_MOVE_DISTANCE_X = 10
        private const val MIN_MOVE_DISTANCE_Y = 8
    }
}