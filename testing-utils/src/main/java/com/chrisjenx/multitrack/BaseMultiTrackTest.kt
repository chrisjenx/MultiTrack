@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.chrisjenx.multitrack

abstract class BaseMultiTrackTest {
// TODO work out a base impl to test all multitrack objects
//
//    private lateinit var multitrack: MultiTrack<>
//    private val converter = object : MultiTrack.Converter<Task> {
//        override fun toValue(source: Source): Task {
//            return source.buffer().use { Task(it.readInt()) }
//        }
//
//        override fun fromValue(value: Task, sink: Sink) {
//            sink.buffer().use { it.writeInt(value.id) }
//        }
//    }
//
//    @Before
//    fun setupBase() {
//
//    }
//
//    private val default = MultiTrack(
//        scheduler = Schedulers.trampoline(), driver = InMemoryDriver(), converter = converter
//    )
//
//    @Test
//    fun push() {
//        default.push(Task(1)).test().awaitDone(1, TimeUnit.SECONDS)
//        assertThat(default.size().blockingGet()).isEqualTo(1)
//    }
//
//    @Test
//    fun poll() {
//        val expected = Task(1)
//
//        default.push(expected).blockingGet()
//        val actual = default.poll().blockingGet()
//
//        assertThat(actual).isEqualTo(expected)
//        assertThat(default.size().blockingGet()).isEqualTo(0)
//    }
//
//    @Test
//    fun peek() {
//        val expected = Task(1)
//
//        default.push(expected).blockingGet()
//        val actual = default.peek().blockingGet()
//
//        assertThat(actual).isEqualTo(expected)
//        assertThat(default.size().blockingGet()).isEqualTo(1)
//    }
//
//    @Test
//    fun size() {
//        assertThat(default.size().blockingGet()).isEqualTo(0)
//
//        default.push(Task(1)).blockingGet()
//        default.push(Task(1)).blockingGet()
//
//        assertThat(default.size().blockingGet()).isEqualTo(2)
//    }
//
//    @Test
//    fun streamSize() {
//        val collection = mutableListOf<Int>()
//        default.streamSize()
//            .doOnNext {
//                collection += it
//                if (it < 3) default.push(Task(it + 1)).subscribe()
//            }
//            .take(4)
//            .test()
//            .awaitTerminalEvent()
//
//        assertThat(collection).containsExactly(0, 1, 2, 3)
//    }
}