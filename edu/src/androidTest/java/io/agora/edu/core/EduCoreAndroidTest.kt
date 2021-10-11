package io.agora.edu.core

import androidx.test.runner.AndroidJUnit4
import io.agora.edu.sdk.app.activities.LargeClassActivity
import io.agora.edu.sdk.app.activities.OneToOneClassActivity
import io.agora.edu.sdk.app.activities.SmallClassActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EduCoreAndroidTest {
    @Test
    fun classReplaceTest() {
        // Class ids are fake data, not what actually used in classrooms
        ClassInfoCache.addRoomActivityDefault(1, OneToOneClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(2, SmallClassActivity::class.java)
        ClassInfoCache.addRoomActivityDefault(3, LargeClassActivity::class.java)

        var clz = ClassInfoCache.getRoomActivityDefault(1)
        assert(clz != null && clz.canonicalName?.equals(OneToOneClassActivity::class.java.canonicalName) ?: false)

        clz = ClassInfoCache.getRoomActivityDefault(2)
        assert(clz != null && clz.canonicalName?.equals(SmallClassActivity::class.java.canonicalName) ?: false)

        clz = ClassInfoCache.getRoomActivityDefault(3)
        assert(clz != null && clz.canonicalName?.equals(LargeClassActivity::class.java.canonicalName) ?: false)

        ClassInfoCache.replaceRoomActivity(1, LargeClassActivity::class.java)
        ClassInfoCache.replaceRoomActivity(2, OneToOneClassActivity::class.java)
        ClassInfoCache.replaceRoomActivity(3, SmallClassActivity::class.java)

        clz = ClassInfoCache.getRoomActivityReplaced(1)
        assert(clz != null && clz.canonicalName?.equals(LargeClassActivity::class.java.canonicalName) ?: false)

        clz = ClassInfoCache.getRoomActivityReplaced(2)
        assert(clz != null && clz.canonicalName?.equals(OneToOneClassActivity::class.java.canonicalName) ?: false)

        clz = ClassInfoCache.getRoomActivityReplaced(3)
        assert(clz != null && clz.canonicalName?.equals(SmallClassActivity::class.java.canonicalName) ?: false)
    }
}