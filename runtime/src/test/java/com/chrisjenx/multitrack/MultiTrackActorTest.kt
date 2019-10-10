@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.chrisjenx.multitrack

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertFailsWith

class MultiTrackActorTest {

    private val scope = TestCoroutineScope()

    private val driver = InMemoryDriver()
    private val converter = TaskConverter()
    private val multiTrack: MultiTrackActor<Task> = MultiTrackActor(
        scope = scope,
        driver = driver,
        converter = converter
    )

    @Test
    fun push() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        val count = multiTrack.size().await()

        assertThat(count).isEqualTo(1)
    }

    @Test
    fun size() = runBlockingTest {
        assertThat(multiTrack.size().await()).isEqualTo(0)
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(1)).await()
        assertThat(multiTrack.size().await()).isEqualTo(2)
    }

    @Test
    fun streamSize() = runBlockingTest {
        val collection = multiTrack.streamSize()
            .take(4)
            .onEach { multiTrack.push(Task(it + 1)).await() }
            .toList()

        assertThat(collection).containsExactly(0, 1, 2, 3)
    }

    @Test
    fun peek() = runBlockingTest {
        val expected = Task(1)
        multiTrack.push(expected).await()

        val result = multiTrack.peek().await()

        assertThat(result).isEqualTo(expected)
        assertThat(multiTrack.size().await()).isEqualTo(1)
    }

    @Test
    fun `peekList no limit`() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(2)).await()
        multiTrack.push(Task(3)).await()

        val result = multiTrack.peekList().await()

        assertThat(result.size).isEqualTo(3)
        assertThat(result).containsExactly(Task(1), Task(2), Task(3))
    }

    @Test
    fun `peekList limit`() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(2)).await()
        multiTrack.push(Task(3)).await()

        val result = multiTrack.peekList(2).await()

        assertThat(result.size).isEqualTo(2)
        assertThat(result).containsExactly(Task(1), Task(2))
    }

    @Test
    fun poll() = runBlockingTest {
        val expected = Task(1)
        multiTrack.push(expected).await()

        val result = multiTrack.poll().await()

        assertThat(result).isEqualTo(expected)
        assertThat(multiTrack.size().await()).isEqualTo(0)
    }

    @Test
    fun remove() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(2)).await()
        multiTrack.push(Task(3)).await()

        assertThat(multiTrack.remove().await()).isTrue()
        assertThat(multiTrack.poll().await()).isEqualTo(Task(2))
    }

    @Test
    fun `remove all`() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(2)).await()
        multiTrack.push(Task(3)).await()

        assertThat(multiTrack.remove(null).await()).isEqualTo(3)
    }

    @Test
    fun `remove many`() = runBlockingTest {
        multiTrack.push(Task(1)).await()
        multiTrack.push(Task(2)).await()
        multiTrack.push(Task(3)).await()
        multiTrack.push(Task(4)).await()

        assertThat(multiTrack.remove(2).await()).isEqualTo(2)
        assertThat(multiTrack.size().await()).isEqualTo(2)
    }

    @Test
    fun `error doesn't kill stream`() = runBlockingTest {
        multiTrack.push(Task(1)).await()

        assertThat(multiTrack.size().await()).isEqualTo(1)

        // Close the Driver all requests should throw ClosedEx
        driver.close()

        // try and read from the queue
        assertFailsWith(ClosedException::class) {
            multiTrack.size().await()
        }

        // try and read from the queue
        assertFailsWith(ClosedException::class) {
            multiTrack.size().await()
        }

        assertFailsWith(ClosedException::class) {
            multiTrack.streamSize().collect { }
        }
    }

    @Test
    fun close() = runBlockingTest {
        multiTrack.close()

        // Should not kill the passed in scope
        assertThat(scope.isActive).isTrue()

        // But should throw cancelled ex
        assertFailsWith(CancellationException::class) {
            multiTrack.push(Task(1)).await()
        }
    }

    @Test
    fun `converter error is propergated`() = runBlockingTest {
        // No error
        multiTrack.push(Task(5)).await()

        // IO Ex happens
        converter.throwError = true
        assertFailsWith(IOException::class) {
            multiTrack.push(Task(10)).await()
        }

        // Try again
        converter.throwError = false
        multiTrack.push(Task(10)).await()

        // Only added 2 because 1 failed
        assertThat(multiTrack.size().await()).isEqualTo(2)
    }
}