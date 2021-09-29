package io.agora.agoraeducore.core.internal.server.requests.http.retrofit

import io.agora.agoraeducore.core.internal.server.requests.Request
import io.agora.agoraeducore.core.internal.server.requests.RequestCallback
import io.agora.agoraeducore.core.internal.server.requests.RequestConfig
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch.*
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch.BoardServiceDispatcher
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch.MediaServiceDispatcher
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch.RoomServiceDispatcher
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch.UserServiceDispatcher
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.*
import io.agora.agoraeducore.core.internal.server.requests.http.retrofit.services.deprecated.BoardService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

internal class RetrofitManager(baseUrl: String, logger: HttpLoggingInterceptor.Logger) {
    private val tag = "RetrofitManager"
    private val timeout = 30L

    private val generalService: GeneralService
    private val roomService: RoomService
    private val userService: UserService
    private val boardService: BoardService
    private val mediaService: MediaService
    private val extService: ExtensionService
    private val messageService: MessageService

    private val roomServiceDispatcher: RoomServiceDispatcher
    private val userServiceDispatcher: UserServiceDispatcher
    private val boardServiceDispatcher: BoardServiceDispatcher
    private val mediaServiceDispatcher: MediaServiceDispatcher
    private val messageServiceDispatcher: MessageServiceDispatcher
    private val extensionServiceDispatcher: ExtensionServiceDispatcher

    private val headerMap = mutableMapOf<String, String>()

    init {
        val clientBuilder = OkHttpClient.Builder()
        clientBuilder.connectTimeout(timeout, TimeUnit.SECONDS)
        clientBuilder.readTimeout(timeout, TimeUnit.SECONDS)
        clientBuilder.addInterceptor(HttpLoggingInterceptor(logger)
                .setLevel(HttpLoggingInterceptor.Level.BODY))
        clientBuilder.addInterceptor(Interceptor { chain ->
            val request = chain.request()
            val builder = request.newBuilder().method(request.method, request.body)
            headerMap.forEach {
                builder.addHeader(it.key, it.value)
            }
            chain.proceed(builder.build())
        })

        val client = clientBuilder.build()
        val retrofit = Retrofit.Builder()
                .client(client)
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        generalService = retrofit.create(GeneralService::class.java)
        roomService = retrofit.create(RoomService::class.java)
        userService = retrofit.create(UserService::class.java)
        mediaService = retrofit.create(MediaService::class.java)
        extService = retrofit.create(ExtensionService::class.java)
        boardService = retrofit.create(BoardService::class.java)
        messageService = retrofit.create(MessageService::class.java)

        roomServiceDispatcher = RoomServiceDispatcher(roomService)
        userServiceDispatcher = UserServiceDispatcher(userService)
        boardServiceDispatcher = BoardServiceDispatcher(boardService)
        mediaServiceDispatcher = MediaServiceDispatcher(mediaService)
        messageServiceDispatcher = MessageServiceDispatcher(messageService)
        extensionServiceDispatcher = ExtensionServiceDispatcher(extService)
    }

    fun addHeader(key: String, value: String) {
        synchronized(this) {
            headerMap[key] = value
        }
    }

    fun send(config: RequestConfig, callback: RequestCallback<Any>?, vararg args: Any?) {
        when (config.request) {
            Request.RoomConfig,
            Request.RoomPreCheck,
            Request.RoomJoin,
            Request.RoomSnapshot,
            Request.RoomSequence,
            Request.RoomSetProperty,
            Request.RoomRemoveProperty,
            Request.RoomSetRoleMuteState,
            Request.RoomSetClassState-> {
                roomServiceDispatcher.dispatch(config, callback, args)
            }

            Request.SendRoomMessage,
            Request.GetRoomMessage,
            Request.SendRoomCustomMessage,
            Request.SendPeerMessage,
            Request.SendPeerCustomMessage,
            Request.SendConversationMessage,
            Request.GetConversationMessage,
            Request.Translate,
            Request.SetUserChatMuteState -> {
                messageServiceDispatcher.dispatch(config, callback, args)
            }

            Request.HandsUpApply,
            Request.HandsUpCancel,
            Request.HandsUpExit -> {
                userServiceDispatcher.dispatch(config, callback, args)
            }

            Request.UpdateDeviceState -> {
                mediaServiceDispatcher.dispatch(config, callback, args)
            }

            Request.SetFlexibleRoomProperty,
            Request.SetFlexibleUserProperty -> {
                extensionServiceDispatcher.dispatch(config, callback, args)
            }
        }
    }
}