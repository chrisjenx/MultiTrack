@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.chrisjenx.multitrack

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Test

class DefaultMultiTrackTest {

    private val dispatcher = TestCoroutineDispatcher()
    private val default = MultiTrack(
        executor = dispatcher.asExecutor(), driver = InMemoryDriver(), converter = TaskConverter()
    )

    @Test
    fun push() {
        default.push(Task(1)).await()
        assertThat(default.size().await()).isEqualTo(1)
    }

    @Test
    fun poll() {
        val expected = Task(1)

        default.push(expected).await()
        val actual = default.poll().await()

        assertThat(actual).isEqualTo(expected)
        assertThat(default.size().await()).isEqualTo(0)
    }

    @Test
    fun peek() {
        val expected = Task(1)

        default.push(expected).await()
        val actual = default.peek().await()

        assertThat(actual).isEqualTo(expected)
        assertThat(default.size().await()).isEqualTo(1)
    }

    @Test
    fun size() {
        assertThat(default.size().await()).isEqualTo(0)

        default.push(Task(1)).await()
        default.push(Task(1)).await()

        assertThat(default.size().await()).isEqualTo(2)
    }

    @Test
    fun streamSize() {
        val collection = mutableListOf<Int>()
        val sub = default.streamSize().observer {
            collection += it
            if (it < 3) default.push(Task(it + 1))
        }
        assertThat(collection).containsExactly(0, 1, 2, 3)
        sub.dispose()
    }

    @Test
    fun remove() {
        default.push(Task(1)).await()
        default.push(Task(2)).await()

        assertThat(default.remove().await())
        assertThat(default.size().await()).isEqualTo(1)
        assertThat(default.poll().await()).isEqualTo(Task(2))
    }

    @Test
    fun `remove all`() {
        default.push(Task(1)).await()
        default.push(Task(2)).await()
        default.push(Task(3)).await()

        assertThat(default.remove(null).await())
        assertThat(default.size().await()).isEqualTo(0)
        assertThat(default.poll().await()).isNull()
    }

    @Test
    fun `remove many`() {
        default.push(Task(1)).await()
        default.push(Task(2)).await()
        default.push(Task(3)).await()

        assertThat(default.remove(2).await())
        assertThat(default.size().await()).isEqualTo(1)
        assertThat(default.poll().await()).isEqualTo(Task(3))
    }

    @Test
    fun mutate() {
        default.push(Task(1))
        default.push(Task(2))
        default.push(Task(3))
        default.push(Task(4))
        default.push(Task(5))

        assertThat(default.size().await()).isEqualTo(5)
        assertThat(default.mutate { it.copy(id = it.id * 10) }.await()).isEqualTo(5)
        val list = default.peekList().await()
        assertThat(list).hasSize(5)
        assertThat(list[0]).isEqualTo(Task(10))
        assertThat(list[1]).isEqualTo(Task(20))
        assertThat(list[2]).isEqualTo(Task(30))
        assertThat(list[3]).isEqualTo(Task(40))
        assertThat(list[4]).isEqualTo(Task(50))
    }
}