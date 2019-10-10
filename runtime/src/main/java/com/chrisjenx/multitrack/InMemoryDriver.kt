@file:Suppress("ForEachParameterNotUsed")

package com.chrisjenx.multitrack

import java.util.ArrayDeque
import java.util.Deque

class InMemoryDriver : MultiTrack.Driver {

    private var queue: Deque<ByteArray>? = ArrayDeque()

    override fun peek(limit: Int?): List<ByteArray> {
        val queue = queue ?: throw ClosedException()
        if (queue.isEmpty()) return emptyList()
        return queue.iterator()
            .asSequence()
            .let { stream -> limit?.let { stream.take(it) } ?: stream }
            .toList()
    }

    override fun poll(): ByteArray? {
        val queue = queue ?: throw ClosedException()
        return queue.pollFirst()
    }

    override fun write(blob: ByteArray): Boolean {
        val queue = queue ?: throw ClosedException()
        return queue.offerLast(blob)
    }

    override fun remove(limit: Int?): Int {
        val queue = queue ?: throw ClosedException()
        if (limit == null) {
            val size = queue.size
            queue.clear()
            return size
        }
        val iterator = queue.iterator()
        return (1..limit)
            .asSequence()
            .takeWhile { iterator.hasNext() }
            .map {
                iterator.next()
                iterator.remove()
            }
            .count()
    }

    override fun count(): Int {
        val queue = queue ?: throw ClosedException()
        return queue.size
    }

    override fun mutate(mapper: (byteArray: ByteArray) -> ByteArray): Int {
        val queue = queue ?: throw ClosedException()
        val mutated = queue.iterator()
            .asSequence()
            .map { mapper(it) }
            .toList()
        queue.clear()
        return mutated.count { queue.offerLast(it) }
    }

    override fun close() {
        queue = null
    }
}
