package io.agora.edu.core.internal.server.requests.rtm

import io.agora.edu.core.internal.server.requests.Request
import io.agora.edu.core.internal.server.requests.RequestChannelPriority
import io.agora.edu.core.internal.server.requests.RequestConfig
import io.agora.edu.core.internal.server.requests.RequestParam
import org.junit.Test

class RtmRequestTest {
    @Test
    fun requestBuildTest() {
        val regex = RequestBuilder.regex
        println("Regular expression ${regex.pattern}")
        var test = "${'{'}userId${'}'}"
        println("Regular expression ${regex.pattern} " +
                "is the format of $test, ${test.matches(regex)}")
        test = "abcd"
        println("Regular expression ${regex.pattern} " +
                "is the format of $test, ${test.matches(regex)}")
        test="{sss}"
        println("Regular expression ${regex.pattern} " +
                "is the format of $test, ${test.matches(regex)}")
        test="$${'}'}   "
        println("Regular expression ${regex.pattern} " +
                "is the format of $test, ${test.matches(regex)}")

        val url = "https://api.agora.com/${'{'}appId${'}'}/route/${'{'}userId${'}'}/business"
        println("source url string $url")

        var result1 = RequestBuilder.replacePlaceholders(url)
        println("replace result $result1")
        assert(result1 == url)

        result1 = RequestBuilder.replacePlaceholders(url, "1234")
        println("replace result $result1")
        println("replace fail because not enough arguments passed")
        assert(result1 == url)

        result1 = RequestBuilder.replacePlaceholders(url, "1234", "alice")
        println("replace result $result1")
        assert(result1 == "https://api.agora.com/1234/route/alice/business")

        result1 = RequestBuilder.replacePlaceholders(url, "4321", "brian", "business")
        println("replace result $result1")
        assert(result1 == "https://api.agora.com/4321/route/brian/business")
    }

    @Test
    fun requestUrlReplaceTest() {
        val config1 = RequestConfig(
                request = Request.RoomSequence,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "scene/apps/{appId}/v1/rooms/{roomUuid}/sequences?nextId={nextId}&count={count}",
                httpMethod = "GET",
                pathCount = 2,
                queryCount = 2)

        val param1 = RequestParam(
                region = "CN",
                tsInMilliSecond = System.currentTimeMillis())

        val param2 = RequestParam(
                region = "CN",
                tsInMilliSecond = System.currentTimeMillis(),
                pathValues = mutableListOf("appId0", "roomId1"))

        val param3 = RequestParam(
                region = "CN",
                tsInMilliSecond = System.currentTimeMillis(),
                pathValues = mutableListOf("appId0", "roomId1"),
                queryValues = mutableListOf("1111", "50"))

        val param4 = RequestParam(
                region = "CN",
                tsInMilliSecond = System.currentTimeMillis(),
                pathValues = mutableListOf("appId0"),
                queryValues = mutableListOf("1111"))

        assert(RequestBuilder.replaceAllPlaceholders(config1.urlFormat, param1) == config1.urlFormat)
        assert(RequestBuilder.replaceAllPlaceholders(config1.urlFormat, param2) == config1.urlFormat)
        assert(RequestBuilder.replaceAllPlaceholders(config1.urlFormat, param3) ==
                "scene/apps/appId0/v1/rooms/roomId1/sequences?nextId=1111&count=50")
        assert(RequestBuilder.replaceAllPlaceholders(config1.urlFormat, param4) == config1.urlFormat)
    }

    @Test
    fun transformTest() {
        val trans1 = ClassTransform("trans1", mutableListOf("test1", "test12"))
        val map1 = RequestBuilder.classToMap(trans1)
        assert(RequestBuilder.classToString(map1) ==
                "{\"id\":\"trans1\",\"keys\":[\"test1\",\"test12\"]}")
    }

    internal class ClassTransform(
            val id: String,
            val keys: MutableList<String>
    )
}