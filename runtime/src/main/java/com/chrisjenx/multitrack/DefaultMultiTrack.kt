@file:Suppress("FunctionName")

package com.chrisjenx.multitrack

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor

class DefaultMultiTrack<T : Any>
internal constructor(
    private val scope: CoroutineScope,
    private val actor: MultiTrackActor<T>
) : MultiTrack<T, Result<Boolean>, Result<Int>, ResultStream<Int>, Result<T?>, Result<List<T>>> {
    override fun push(entry: T): Result<Boolean> {
        return actor.push(entry).createResult(scope)
    }

    override fun poll(): Result<T?> {
        return actor.poll().createResult(scope)
    }

    override fun peek(): Result<T?> {
        return actor.peek().createResult(scope)
    }

    override fun peekList(limit: Int?): Result<List<T>> {
        return actor.peekList(limit).createResult(scope)
    }

    override fun size(): Result<Int> {
        return actor.size().createResult(scope)
    }

    override fun streamSize(): ResultStream<Int> {
        return actor.streamSize().createResultStream(scope)
    }

    override fun remove(): Result<Boolean> {
        return actor.remove().createResult(scope)
    }

    override fun remove(limit: Int?): Result<Int> {
        return actor.remove(limit).createResult(scope)
    }

    override fun mutate(converter: (T) -> T): Result<Int> {
        return actor.mutate(converter).createResult(scope)
    }

    override fun close() {
        actor.close()
    }
}

/**
 * Create a MultiTrack which exposes plain kotlin based callbacks
 */
fun <T : Any> MultiTrack(
    executor: Executor,
    driver: MultiTrack.Driver = InMemoryDriver(),
    converter: MultiTrack.Converter<T>
): DefaultMultiTrack<T> {
    val dispatcher = executor.asCoroutineDispatcher()
    val scope = CoroutineScope(SupervisorJob() + dispatcher)
    val actor = MultiTrackActor(scope, driver, converter)
    return DefaultMultiTrack(scope, actor)
}