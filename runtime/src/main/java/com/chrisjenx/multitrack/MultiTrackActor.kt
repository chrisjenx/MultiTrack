@file:Suppress("EXPERIMENTAL_API_USAGE", "DeferredIsResult")

package com.chrisjenx.multitrack

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.selects.select
import okio.Buffer
import java.util.concurrent.CancellationException

class MultiTrackActor<T : Any>
constructor(
    scope: CoroutineScope,
    private val driver: MultiTrack.Driver,
    private val converter: MultiTrack.Converter<T>
) : CoroutineMultiTrack<T> {
    // Scope for this Object Specifically
    private val supervisor = SupervisorJob()
    private val scope: CoroutineScope = scope + supervisor
    /**
     * Requests for reading from the queue
     */
    private val readActions = Channel<ReadAction<*>>(Channel.UNLIMITED)
    /**
     * Requests for writing to the queue
     */
    private val writeActions = Channel<WriteAction<*>>(Channel.UNLIMITED)
    /**
     * Channel that gets fired after an write has happened.
     */
    private val updates = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.initActor(readActions, writeActions, updates)
    }

    override fun size(): Deferred<Int> {
        val callback = CompletableDeferred<Int>(supervisor)
        scope.launch { readActions.send(Size(callback)) }
        return callback
    }

    override fun streamSize(): Flow<Int> {
        return updates
            .consumeAsFlow()
            .onStart { emit(Unit) }
            .map { size().await() }
    }

    override fun push(entry: T): Deferred<Boolean> {
        val callback = CompletableDeferred<Boolean>(supervisor)
        scope.launchPropEx(callback) {
            val ba = Buffer().use { converter.fromValue(entry, it); it.readByteArray() }
            writeActions.send(Push(ba, callback))
        }
        return callback
    }

    override fun peek(): Deferred<T?> {
        val callback = CompletableDeferred<T?>(supervisor)
        scope.launchPropEx(callback) {
            val deferred = CompletableDeferred<List<ByteArray>>()
            readActions.send(Peek(1, deferred))
            callback.deferEx {
                deferred.await().firstOrNull()
                    ?.let { Buffer().use { buffer -> buffer.write(it); converter.toValue(buffer) } }
            }
        }
        return callback
    }

    override fun peekList(limit: Int?): Deferred<List<T>> {
        val callback = CompletableDeferred<List<T>>(supervisor)
        scope.launchPropEx(callback) {
            val deferred = CompletableDeferred<List<ByteArray>>()
            readActions.send(Peek(limit, deferred))
            callback.deferEx {
                Buffer().use { buffer ->
                    deferred.await().map { buffer.clear(); buffer.write(it); converter.toValue(buffer) }
                }
            }
        }
        return callback
    }

    override fun poll(): Deferred<T?> {
        val callback = CompletableDeferred<T?>()
        scope.launchPropEx(callback) {
            val deferred = CompletableDeferred<ByteArray?>(supervisor)
            writeActions.send(Poll(deferred))
            callback.deferEx {
                val bytes = deferred.await() ?: return@deferEx null
                Buffer().use { it.write(bytes); converter.toValue(it) }
            }
        }
        return callback
    }

    override fun remove(): Deferred<Boolean> {
        val callback = CompletableDeferred<Boolean>(supervisor)
        scope.launchPropEx(callback) {
            val deferred = CompletableDeferred<Int>()
            writeActions.send(Remove(1, deferred))
            callback.deferEx { deferred.await() == 1 }
        }
        return callback
    }

    override fun remove(limit: Int?): Deferred<Int> {
        val callback = CompletableDeferred<Int>(supervisor)
        scope.launchPropEx(callback) { writeActions.send(Remove(limit, callback)) }
        return callback
    }

    override fun mutate(converter: (T) -> T): Deferred<Int> {
        val typeConverter = this.converter
        val mutator = { input: ByteArray ->
            Buffer().use {
                it.write(input)
                val current = typeConverter.toValue(it)
                val mutated = converter(current)
                it.clear()
                typeConverter.fromValue(mutated, it)
                it.readByteArray()
            }
        }

        val callback = CompletableDeferred<Int>(supervisor)
        scope.launchPropEx(callback) { writeActions.send(Mutate(mutator, callback)) }
        return callback
    }

    private fun CoroutineScope.initActor(
        readActions: ReceiveChannel<ReadAction<*>>,
        writeActions: ReceiveChannel<WriteAction<*>>,
        updates: SendChannel<Unit>
    ) = launch {
        while (supervisor.isActive) {
            select<Unit> {
                readActions.onReceive { read ->
                    when (read) {
                        is Peek -> read.callback.deferEx { driver.peek(read.limit) }
                        is Size -> read.callback.deferEx { driver.count() }
                    }
                }
                writeActions.onReceive { write ->
                    when (write) {
                        is Push -> write.callback.deferEx({ if (it) updates.send(Unit) }, { driver.write(write.bytes) })
                        is Poll -> write.callback.deferEx({ if (it != null) updates.send(Unit) }) { driver.poll() }
                        is Remove -> {
                            write.callback.deferEx({ if (it > 0) updates.send(Unit) }, { driver.remove(write.limit) })
                        }
                        is Mutate -> {
                            write.callback.deferEx({ if (it > 0) updates.send(Unit) }, { driver.mutate(write.mutator) })
                        }
                    }
                }
            }
        }
    }

    override fun close() {
        driver.close()
        val ex = CancellationException("Closed")
        supervisor.cancel(ex)
        readActions.close(ex)
        writeActions.close(ex)
    }
}

typealias CoroutineMultiTrack<T> = MultiTrack<T, Deferred<Boolean>, Deferred<Int>, Flow<Int>, Deferred<T?>, Deferred<List<T>>>