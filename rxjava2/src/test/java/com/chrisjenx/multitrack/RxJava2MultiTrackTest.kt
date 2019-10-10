@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.chrisjenx.multitrack

import com.google.common.truth.Truth.assertThat
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.util.concurrent.TimeUnit

class RxJava2MultiTrackTest {

    private val default = MultiTrack(
        scheduler = Schedulers.trampoline(), driver = InMemoryDriver(), converter = TaskConverter()
    )

    @Test
    fun push() {
        default.push(Task(1)).test().awaitDone(1, TimeUnit.SECONDS)
        assertThat(default.size().blockingGet()).isEqualTo(1)
    }

    @Test
    fun poll() {
        val expected = Task(1)

        default.push(expected).blockingGet()
        val actual = default.poll().blockingGet()

        assertThat(actual).isEqualTo(expected)
        assertThat(default.size().blockingGet()).isEqualTo(0)
    }

    @Test
    fun peek() {
        val expected = Task(1)

        default.push(expected).blockingGet()
        val actual = default.peek().blockingGet()

        assertThat(actual).isEqualTo(expected)
        assertThat(default.size().blockingGet()).isEqualTo(1)
    }

    @Test
    fun size() {
        assertThat(default.size().blockingGet()).isEqualTo(0)

        default.push(Task(1)).blockingGet()
        default.push(Task(1)).blockingGet()

        assertThat(default.size().blockingGet()).isEqualTo(2)
    }

    @Test
    fun streamSize() {
        val collection = mutableListOf<Int>()
        default.streamSize()
            .doOnNext {
                collection += it
                if (it < 3) default.push(Task(it + 1)).subscribe()
            }
            .take(4)
            .test()
            .awaitTerminalEvent()

        assertThat(collection).containsExactly(0, 1, 2, 3)
    }
}