package io.agora.agoraeduuikit.impl.extapps.iclicker

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.DOT
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_ANSWER
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_END_TIME
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_ITEMS
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_START_TIME
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STATE
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENT
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENTNAMES
import io.agora.agoraeduuikit.impl.extapps.iclicker.IClickerStatics.PROPERTIES_KEY_STUDENTS

data class IClickerStatus(
        val answer: MutableList<String>? = null,
        val canChange: Boolean? = null,
        val endTime: String? = null,
        val items: MutableList<String>? = null,
        val mulChoice: Boolean = true,
        val startTime: String? = null,
        val state: String? = null,
        val studentNames: MutableList<String>? = null,
        val students: MutableList<String>? = null,
) {
    @Transient
    private val studentAnswers = mutableMapOf<String, ReplyItem>()

    /**
     * @param uid: "student" + studentUserUuid
     * */
    fun addStudentAnswer(uid: String, item: ReplyItem) {
        studentAnswers[uid] = item
    }

    fun addStudentAnswer(map: MutableMap<String, ReplyItem?>) {
        map.forEach {
            it.value?.let { item ->
                addStudentAnswer(it.key, item)
            }
        }
    }

    fun addStudentAnswer(entry: MutableMap.MutableEntry<String, ReplyItem?>) {
        entry.value?.let {
            addStudentAnswer(entry.key, it)
        }
    }

    fun convert(): MutableMap<String, Any?>? {
        val json = Gson().toJson(this)
        val map: MutableMap<String, Any?>? = Gson().fromJson(json, object : TypeToken<MutableMap<String, Any?>?>() {}.type)
        return map?.apply {
            this.putAll(studentAnswers)
        }
    }

    companion object {
        fun convert(json: String): IClickerStatus {
            val studentReplyItems = mutableMapOf<String, ReplyItem?>()
            val map: MutableMap<String, Any?>? = Gson().fromJson(json,
                    object : TypeToken<MutableMap<String, Any?>?>() {}.type)
            map?.filter { it.key.startsWith(PROPERTIES_KEY_STUDENT) }?.forEach {
                if (!it.key.startsWith(PROPERTIES_KEY_STUDENTNAMES) && !it.key.startsWith(PROPERTIES_KEY_STUDENTS)) {
                    val itemJson = Gson().toJson(it.value)
                    val replyItem = Gson().fromJson(itemJson, ReplyItem::class.java)
                    studentReplyItems[it.key] = replyItem
                }
            }
            val status = Gson().fromJson(json, IClickerStatus::class.java)
            status.addStudentAnswer(studentReplyItems)
            return status
        }

        fun buildDelKeys(identifier: String, students: MutableList<*>?): MutableList<String> {
            val delKeys = mutableListOf<String>()
            val prefix = identifier.plus(DOT)
            delKeys.add(prefix.plus(PROPERTIES_KEY_ANSWER))
            delKeys.add(prefix.plus(PROPERTIES_KEY_END_TIME))
            delKeys.add(prefix.plus(PROPERTIES_KEY_ITEMS))
            delKeys.add(prefix.plus(PROPERTIES_KEY_START_TIME))
            delKeys.add(prefix.plus(PROPERTIES_KEY_STATE))
            delKeys.add(prefix.plus(PROPERTIES_KEY_STUDENTNAMES))
            delKeys.add(prefix.plus(PROPERTIES_KEY_STUDENTS))
            students?.forEach {
                val uuid = it.toString()
                delKeys.add(prefix.plus(PROPERTIES_KEY_STUDENT).plus(uuid))
            }
            return delKeys
        }
    }
}

object IClickerAnswerState {
    const val start = "start"
    const val end = "end"
}