package io.agora.agoraeducore.core.internal.server.requests

import io.agora.agoraeducore.core.internal.server.struct.request.EduJoinClassroomReq
import org.junit.Test

class RequestTest {
    private interface A {

    }

    private open class AExt : A {

    }

    private class ATypes<T> : AExt() {

    }

    @Test
    fun requestFunctionTest() {
        println("start param check")

        // Test only config parameters check
        // Particularly, keep the param count and the size of param type array the same size
        val config1 = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "",
                httpMethod = "POST")

        val config2 = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "",
                httpMethod = "POST",
                pathCount = 1)

        val config3 = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "",
                httpMethod = "POST",
                pathCount = 3,
                hasBody = true,
                bodyType = EduJoinClassroomReq::class)

        val config4 = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "",
                httpMethod = "POST",
                headerCount = 1,
                headerKeys = listOf("userToken"),
                pathCount = 2,
                queryCount = 1,
                hasBody = true,
                bodyType = String::class)

        println("empty space is an instance of String class ${String::class.isInstance("")}")
        println("Null pointer is an instance of String class ${String::class.isInstance(null)}")
        println("Map is the same type as MutableMap ${MutableMap::class.isInstance(mapOf<String, String>())}")

        assert(Request.isValidArguments(config1))
        assert(!Request.isValidArguments(config1, 2))

        assert(Request.isValidArguments(config2, ""))
        assert(!Request.isValidArguments(config2))
        assert(!Request.isValidArguments(config2, "", 1))

        assert(!Request.isValidArguments(config3))
        assert(!Request.isValidArguments(config3, "", 2))
        assert(Request.isValidArguments(config3, 1, 2, 3,
                EduJoinClassroomReq("", "", "", 1)))

        assert(!Request.isValidArguments(config4, "", ""))
        assert(!Request.isValidArguments(config4, "token", "", "", mapOf<String, Any>()))
        assert(Request.isValidArguments(config4, "token", "path1", "path2", "", "5"))
    }
}