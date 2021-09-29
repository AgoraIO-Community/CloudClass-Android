package io.agora.agoraeducore.core.internal.server.requests.rtm

import com.google.gson.Gson
import io.agora.agoraeducore.core.internal.server.requests.*

internal object RequestBuilder {
    val regex = Regex("\\{\\w*\\}")

    /**
     * Request parameters mean the runtime values of the keys defined in
     * request configuration each time one request is sent.
     * @param config request config for current request parameter
     * @param region which region around the world that this request want to send,
     * this affects the server that will be chosen
     * @param headers global http request headers
     * @param callback
     * @param args a series of argument of any non-null type, the length
     * of arguments should be the same as the request config. The length is the
     * sum of request headers (request-specific), paths, queries and body
     */
    fun buildParamWithArgs(config: RequestConfig, region: String, headers: Map<String, String>,
                   callback: RequestCallback<Any>?, vararg args: Any) : RequestParam {
        val param = RequestParam(region,
                System.currentTimeMillis(),
                mutableMapOf(),
                mutableListOf(),
                mutableListOf(),
                callback)

        if (!Request.isValidArguments(config, args)) {
            throw IllegalRtmRequestArgumentException("Illegal parameters")
        }

        param.headers.putAll(headers)

        var idx = 0
        for (i in idx until config.headerCount) {
            param.headers[config.headerKeys[i]] = args[i].toString()
        }
        idx += config.headerCount

        for (i in idx until (idx + config.pathCount)) {
            param.pathValues.add(args[i].toString())
        }
        idx += config.pathCount

        for (i in idx until idx + config.queryCount) {
            param.queryValues.add(args[i].toString())
        }
        idx += config.queryCount

        param.body = args[idx]
        param.callback
        return param
    }

    /**
     * Match url string path placeholders, replace those placeholders
     * with the actual arguments
     * @param url the url path that needs to be processed
     * @param args arguments used to replace placeholders. The argument
     * can be more than needed. It that is the case, only the first few
     * will be used and ignore the rest
     * @return a new string that represents the replace result
     */
    internal fun replacePlaceholders(url: String, vararg args: String) : String {
        val holders = findUrlPlaceholders(url)
        if (args.size < holders.size) {
            return url.trim()
        }

        return replacePlaceholders(url, holders, args.toList())
    }

    private fun findUrlPlaceholders(url: String) : List<IntRange> {
        val list = mutableListOf<IntRange>()
        val matchList = regex.findAll(url).toList()
        for (i in matchList.indices) {
            list.add(matchList[i].range)
        }

        return list
    }

    private fun replacePlaceholders(url: String, ranges: List<IntRange>, args: List<String>) : String {
        var temp = url.trim()
        check(ranges.size <= args.size)
        for (i in ranges.size - 1 downTo 0) {
            temp = temp.replaceRange(ranges[i], args[i])
        }
        return temp
    }

    internal fun replaceAllPlaceholders(url: String, param: RequestParam) : String {
        val ranges = findUrlPlaceholders(url)
        val list = mutableListOf<String>()
        param.pathValues.forEach {
            list.add(it)
        }
        param.queryValues.forEach {
            list.add(it)
        }

        return if (ranges.size != list.size) url
        else replacePlaceholders(url, ranges, list)
    }

    internal fun classToMap(target: Any) : Map<String, Any> {
        val json = Gson()
        return json.fromJson(json.toJson(target), Map::class.java) as Map<String, Any>
    }

    internal fun classToString(target: Any) : String {
        return Gson().toJson(target)
    }
}

class IllegalRtmRequestArgumentException(message: String) : Exception(message)