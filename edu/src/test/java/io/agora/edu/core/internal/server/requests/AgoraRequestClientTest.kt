package io.agora.edu.core.internal.server.requests

import org.junit.Test

class AgoraRequestClientTest {
    @Test
    fun testChannelPriority() {
        val config = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.HTTP,
                urlFormat = "",
                httpMethod = "POST")

        val config1 = RequestConfig(
                request = Request.RoomJoin,
                priority = RequestChannelPriority.RTM,
                urlFormat = "",
                httpMethod = "POST")

        assert(AgoraRequestClient.getSendChannel(config,
                RequestChannelPriority.DEFAULT) ==
                RequestChannelPriority.HTTP)

        assert(AgoraRequestClient.getSendChannel(config,
                RequestChannelPriority.HTTP) ==
                RequestChannelPriority.HTTP)

        assert(AgoraRequestClient.getSendChannel(config,
                RequestChannelPriority.RTM) ==
                RequestChannelPriority.RTM)

        assert(AgoraRequestClient.getSendChannel(config1,
                RequestChannelPriority.DEFAULT) ==
                RequestChannelPriority.RTM)

        assert(AgoraRequestClient.getSendChannel(config1,
                RequestChannelPriority.RTM) ==
                RequestChannelPriority.RTM)

        assert(AgoraRequestClient.getSendChannel(config1,
                RequestChannelPriority.HTTP) ==
                RequestChannelPriority.HTTP)
    }
}