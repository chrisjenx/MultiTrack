package com.chrisjenx.multitrack

import kotlinx.coroutines.CompletableDeferred

internal sealed class ReadAction<T : Any?>(val callback: CompletableDeferred<T>)
internal sealed class WriteAction<T : Any?>(val callback: CompletableDeferred<T>)
/**
 * Peek at the first entry on the queue
 *
 * @param limit null is no limit
 * @param callback empty list is no results
 */
internal class Peek(
    val limit: Int? = null,
    callback: CompletableDeferred<List<ByteArray>>
) : ReadAction<List<ByteArray>>(callback)

/**
 * Pull an entry off the queue the front of the queue and remove it.
 */
internal class Poll(
    callback: CompletableDeferred<ByteArray?>
) : WriteAction<ByteArray?>(callback)

/**
 * Get the queue size
 */
internal class Size(callback: CompletableDeferred<Int>) : ReadAction<Int>(callback)

/**
 * Push a new entry to the end of the queue
 */
internal class Push(val bytes: ByteArray, callback: CompletableDeferred<Boolean>) : WriteAction<Boolean>(callback)

/**
 * Remove entry from the head of the queue
 */
internal class Remove(val limit: Int? = null, callback: CompletableDeferred<Int>) : WriteAction<Int>(callback)

/**
 * Mutate the list, head to tail.
 */
internal class Mutate(
    val mutator: (ByteArray) -> ByteArray, callback: CompletableDeferred<Int>
) : WriteAction<Int>(callback)

