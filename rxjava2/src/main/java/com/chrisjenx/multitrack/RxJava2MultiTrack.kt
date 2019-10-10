@file:Suppress("EXPERIMENTAL_API_USAGE", "FunctionName")

package com.chrisjenx.multitrack

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import kotlinx.coroutines.rx2.asMaybe
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.rx2.asSingle
import kotlin.coroutines.CoroutineContext

class RxJava2MultiTrack<T : Any>
internal constructor(
    private val context: CoroutineContext,
    private val actor: MultiTrackActor<T>
) : MultiTrack<T, Single<Boolean>, Single<Int>, Observable<Int>, Maybe<T>, Single<List<T>>> {
    override fun push(entry: T): Single<Boolean> {
        return actor.push(entry).asSingle(context)
    }

    override fun poll(): Maybe<T> {
        return actor.poll().asMaybe(context)
    }

    override fun peek(): Maybe<T> {
        return actor.peek().asMaybe(context)
    }

    override fun peekList(limit: Int?): Single<List<T>> {
        return actor.peekList(limit).asSingle(context)
    }

    override fun size(): Single<Int> {
        return actor.size().asSingle(context)
    }

    override fun streamSize(): Observable<Int> {
        return actor.streamSize().asObservable()
    }

    override fun remove(): Single<Boolean> {
        return actor.remove().asSingle(context)
    }

    override fun remove(limit: Int?): Single<Int> {
        return actor.remove(limit).asSingle(context)
    }

    override fun mutate(converter: (T) -> T): Single<Int> {
        return actor.mutate(converter).asSingle(context)
    }

    override fun close() {
        return actor.close()
    }
}

/**
 * Create a MultiTrack which exposes plain kotlin based callbacks
 */
fun <T : Any> MultiTrack(
    scheduler: Scheduler,
    driver: MultiTrack.Driver = InMemoryDriver(),
    converter: MultiTrack.Converter<T>
): RxJava2MultiTrack<T> {
    val context = scheduler.asCoroutineDispatcher()
    val scope = CoroutineScope(context + SupervisorJob())
    val actor = MultiTrackActor(scope, driver, converter)
    return RxJava2MultiTrack(context, actor)
}
