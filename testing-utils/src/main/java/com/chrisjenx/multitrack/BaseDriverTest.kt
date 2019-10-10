package com.chrisjenx.multitrack

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.internal.commonToUtf8String
import org.junit.After
import org.junit.Before
import org.junit.Test

abstract class BaseDriverTest {

    private lateinit var driver: MultiTrack.Driver

    private val byteString = "Hello".encodeUtf8()
    private val byteString2 = "Hello2".encodeUtf8()
    private val byteString3 = "Hello3".encodeUtf8()

    @Before
    fun setUpBase() {
        driver = createDriver()
    }

    @After
    fun tearDownBase() {
        driver.close()
    }

    abstract fun createDriver(): MultiTrack.Driver

    @Test
    fun write() {
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(1)
    }

    @Test
    fun count() {
        assertThat(driver.count()).isEqualTo(0)
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(3)
    }

    @Test
    fun peek() {
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(2)
        val actual = driver.peek(1).singleOrNull()?.let { ByteString.of(*it) }
        assertThat(actual).isNotNull()
        assertThat(actual).isEquivalentAccordingToCompareTo(byteString)
    }

    @Test
    fun peekList() {
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(3)

        val actual = driver.peek()
        assertThat(actual).isNotNull()
        assertThat(actual.size).isEqualTo(3)
    }

    @Test
    fun `peekList limit`() {
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(3)

        val actual = driver.peek(2)
        assertThat(actual).isNotNull()
        assertThat(actual.size).isEqualTo(2)
    }

    @Test
    fun poll() {
        driver.write(byteString.toByteArray())
        driver.write(byteString.toByteArray())
        assertThat(driver.count()).isEqualTo(2)
        assertThat(driver.poll()?.let { ByteString.of(*it) }).isEquivalentAccordingToCompareTo(byteString)
        assertThat(driver.count()).isEqualTo(1)
        assertThat(driver.poll()?.let { ByteString.of(*it) }).isEquivalentAccordingToCompareTo(byteString)
        assertThat(driver.count()).isEqualTo(0)
    }

    @Test
    fun `remove one`() {
        driver.write(byteString.toByteArray())
        driver.write(byteString2.toByteArray())

        assertThat(driver.remove(1)).isEqualTo(1)
        assertThat(driver.count()).isEqualTo(1)

        assertThat(driver.poll()?.let { ByteString.of(*it) }).isEquivalentAccordingToCompareTo(byteString2)
    }

    @Test
    fun `remove all`() {
        driver.write(byteString.toByteArray())
        driver.write(byteString2.toByteArray())
        driver.write(byteString2.toByteArray())

        assertThat(driver.remove()).isEqualTo(3)
        assertThat(driver.count()).isEqualTo(0)
    }

    @Test
    fun `remove many`() {
        driver.write(byteString.toByteArray())
        driver.write(byteString2.toByteArray())
        driver.write(byteString3.toByteArray())

        assertThat(driver.remove(2)).isEqualTo(2)
        assertThat(driver.count()).isEqualTo(1)

        assertThat(driver.poll()?.let { ByteString.of(*it) }).isEquivalentAccordingToCompareTo(byteString3)
    }

    @Test
    fun `mutate all`() {
        driver.write(byteString.toByteArray())
        driver.write(byteString2.toByteArray())
        val buffer = Buffer()
        val mapper = { input: ByteArray -> buffer.write(input).writeUtf8("Mutated").readByteArray() }

        assertThat(driver.mutate(mapper)).isEqualTo(2)

        // Order should be the same
        val expected1 = buffer.write(byteString).writeUtf8("Mutated").readByteArray()
        val expected2 = buffer.write(byteString2).writeUtf8("Mutated").readByteArray()
        assertThat(driver.peek()[0]).isEqualTo(expected1)
        assertThat(driver.peek()[1]).isEqualTo(expected2)
    }
}