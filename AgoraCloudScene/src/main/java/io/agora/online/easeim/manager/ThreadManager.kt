package io.agora.online.easeim.manager

import android.os.Handler
import android.os.Looper
import android.os.Process
import java.util.concurrent.*

class ThreadManager {
    private var mIOThreadExecutor: Executor? = null
    private var mMainThreadHandler: Handler? = null

    companion object{
        val instance by lazy (LazyThreadSafetyMode.NONE){
            ThreadManager()
        }
    }

    init {
        val NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors()
        val KEEP_ALIVE_TIME = 1L
        val KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS
        val taskQueue: BlockingQueue<Runnable> = LinkedBlockingDeque()
        mIOThreadExecutor = ThreadPoolExecutor(
            NUMBER_OF_CORES,
            NUMBER_OF_CORES * 2,
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            taskQueue,
            BackgroundThreadFactory(Process.THREAD_PRIORITY_BACKGROUND)
        )
        mMainThreadHandler = Handler(Looper.getMainLooper())
    }

    /**
     * 在异步线程执行
     */
    fun runOnIOThread(runnable: () -> Unit) {
        mIOThreadExecutor!!.execute(runnable)
    }

    /**
     * 在UI线程执行
     */
    fun runOnMainThread(runnable: () -> Unit) {
        mMainThreadHandler!!.post(runnable!!)
    }

    /**
     * 判断是否是主线程
     */
    fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread === Thread.currentThread()
    }
}