package io.agora.agoraeducore.core.internal.server.requests.http.retrofit.dispatch

import io.agora.agoraeducore.core.internal.server.requests.RequestCallback
import io.agora.agoraeducore.core.internal.server.requests.RequestError
import io.agora.agoraeducore.core.internal.server.struct.response.BaseResponseBody
import io.agora.agoraeducore.core.internal.server.struct.response.DataResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Basic structures used by service dispatcher implementations.
 * The packed server response callback implementations correspond different
 * callback template types.
 * Take care that there are currently two different kinds of ResponseBody
 * implemented in different package folder but they are the same thing.
 * They are both common ResponseBody whose msg property is String.
 * We keep them both here to make the previous code compatible and easy
 * to refactor, and they will be combined into one later.
 */
abstract class AbsServiceDispatcher : IServiceDispatcher {
    /**
     * Represents a service callback when no basic information returned but
     * only custom data structure.
     */
    protected class ServiceRespCallback<T>(private val callback: RequestCallback<Any>?) : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            response.body()?.let {
                callback?.onSuccess(it)
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            callback?.onFailure(RequestError(-1, t.message ?: ""))
        }
    }

    /**
     * Wrap a server callback where only basic information returned: code, string message and
     * timestamp. Since the response does not contain any custom data, the callback
     * will give the whole response body
     */
    protected class ServiceRespCallbackWithBaseBody(private val callback: RequestCallback<Any>?) : Callback<BaseResponseBody> {
        override fun onResponse(call: Call<BaseResponseBody>, response: Response<BaseResponseBody>) {
            response.body()?.let {
                callback?.onSuccess(it)
            }
        }

        override fun onFailure(call: Call<BaseResponseBody>, t: Throwable) {
            callback?.onFailure(RequestError(-1, t.message ?: ""))
        }
    }

    /**
     * Wrap a server callback where contains a request-specific response data.
     * The callback will give only the custom response data, instead
     */
    protected class ServiceRespCallbackWithDataBody<T>(private val callback: RequestCallback<Any>?) : Callback<DataResponseBody<T>> {
        override fun onResponse(call: Call<DataResponseBody<T>>, response: Response<DataResponseBody<T>>) {
            response.body()?.let {
                callback?.onSuccess(it.data)
            }
        }

        override fun onFailure(call: Call<DataResponseBody<T>>, t: Throwable) {
            callback?.onFailure(RequestError(-1, t.message ?: ""))
        }
    }
}