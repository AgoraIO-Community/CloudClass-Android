package io.agora.privatechat

import io.agora.base.callback.Callback
import io.agora.base.callback.ThrowableCallback

import org.junit.Assert.*
import java.net.HttpURLConnection

class PrivateChatManagerTest {

    var sideVoiceCallMgr: PrivateChatManager? = null

    @org.junit.Before
    fun setUp() {
        sideVoiceCallMgr = PrivateChatManager(null, null, null)
    }

    @org.junit.After
    fun tearDown() {
    }

    @org.junit.Test
    fun startSideVoiceCall() {
        sideVoiceCallMgr!!.startSideVoiceChat(
                "jmmy02",
                object : ThrowableCallback<ResponseBody> {
                    override fun onSuccess(res: ResponseBody?) {
                        print(res?.msg)
                        assertEquals(res?.code, HttpURLConnection.HTTP_OK)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        fail(throwable?.message + " " + throwable?.stackTrace)
                    }
                })
    }

    @org.junit.Test
    fun stopSideVoiceCall() {
        sideVoiceCallMgr!!.stopSideVoiceChat(Callback<ResponseBody> { res ->
            print(res?.msg)
            assertEquals(res?.code, HttpURLConnection.HTTP_OK)
        })
    }
}
