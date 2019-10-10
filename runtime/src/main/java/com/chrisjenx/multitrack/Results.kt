@file:Suppress("NOTHING_TO_INLINE", "EXPERIMENTAL_API_USAGE")

package com.chrisjenx.multitrack

import com.chrisjenx.multitrack.ResultStream.Observer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

interface Result<T> {
    /**
     * Blocking call to wait for the result. Remember if you want a fresh result you need to get a new [Result].
     *
     * @throws Exception if an error occurs trying to do the requested operation
     */
    fun await(): T

    /**
     * Will complete once result has returned.
     */
    fun completed(): Boolean

    /**
     * Attach and observer waiting for the result. Once callback has been emitted the reference is cleared from the
     * result.
     */
    fun observe(observer: Single<T>)

    interface Single<T> {
        fun error(throwable: Throwable)
        fun completed(value: T)
    }
}

internal inline fun <T> Deferred<T>.createResult(scope: CoroutineScope): Result<T> {
    return ResultImpl(scope, this)
}

internal class ResultImpl<T>(
    private val scope: CoroutineScope,
    private val deferred: Deferred<T>
) : Result<T> {

    private var completed: Boolean = false

    init {
        scope.launch {
            deferred.invokeOnCompletion { completed = true }
        }
    }

    private fun start(observer: Result.Single<T>) {
        scope.launch {
            try {
                val result = deferred.await()
                observer.completed(result)
            } catch (e: Exception) {
                observer.error(e)
            }
        }
    }

    override fun await(): T = runBlocking { deferred.await() }

    override fun completed(): Boolean = completed

    override fun observe(observer: Result.Single<T>) {
        start(observer)
    }
}

/**
 * ResultStream provide a cold stream of data.
 *
 * I.e. Each observer will start a separate listener to the data source, they are
 * not shared and will ONLY start once you attach an [Observer].
 */
interface ResultStream<T> {
    fun observer(observer: Observer<T>): Disposable
    fun observer(observer: (T) -> Unit): Disposable
    fun observer(observer: (T) -> Unit, error: (Throwable) -> Unit): Disposable

    interface Observer<T> {
        fun emit(value: T)
        fun error(throwable: Throwable)
    }

    interface Disposable {
        fun dispose()
    }
}

internal class ResultStreamImpl<T>(
    private val scope: CoroutineScope,
    private val flow: Flow<T>
) : ResultStream<T> {
    override fun observer(observer: Observer<T>): ResultStream.Disposable {
        val job = scope.launch {
            try {
                flow.collect { observer.emit(it) }
            } catch (e: Exception) {
                observer.error(e)
            }
        }
        return DisposableImpl(job)
    }

    override fun observer(observer: (T) -> Unit): ResultStream.Disposable {
        return observer(object : Observer<T> {
            override fun emit(value: T) {
                observer(value)
            }

            override fun error(throwable: Throwable) {
                throw NotImplementedError("Observer Error NotImplemented")
            }
        })
    }

    override fun observer(observer: (T) -> Unit, error: (Throwable) -> Unit): ResultStream.Disposable {
        return observer(object : Observer<T> {
            override fun emit(value: T) {
                observer(value)
            }

            override fun error(throwable: Throwable) {
                error(throwable)
            }
        })
    }
}

internal class DisposableImpl(private val job: Job) : ResultStream.Disposable {
    override fun dispose() {
        runBlocking { job.cancelAndJoin() }
    }
}

internal inline fun <T> Flow<T>.createResultStream(scope: CoroutineScope): ResultStream<T> {
    return ResultStreamImpl(scope, this)
}
