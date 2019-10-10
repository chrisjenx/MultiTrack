@file:Suppress("FunctionName")

package com.chrisjenx.multitrack

import okio.BufferedSink
import okio.BufferedSource
import java.io.Closeable

/**
 * This is the main interface to talk to read write with.
 */
interface MultiTrack<T : Any, RBOOL, RINT, SINT, RESULT, RESULT_LIST> {

    /**
     * Push an item onto the end of the queue. True on success, false if it failed to add.
     */
    fun push(entry: T): RBOOL

    /**
     * Get and Remove the first item off the queue if one exists, null if empty
     */
    fun poll(): RESULT

    /**
     * Look at the first item in the list without removing, null if queue is empty
     */
    fun peek(): RESULT

    /**
     * Look at the first [limit] item(s) in the list without removing.
     */
    fun peekList(limit: Int? = null): RESULT_LIST

    /**
     * Get the current size of the queue
     */
    fun size(): RINT

    /**
     * Stream the current size of the queue
     */
    fun streamSize(): SINT

    /**
     * Remove head of the queue, boolean callback whether it's successful.
     */
    fun remove(): RBOOL

    /**
     * Remove up to the [limit] from the queue.
     */
    fun remove(limit: Int?): RINT

    /**
     * Mutate each element on the queue, returns how many items it mutated.
     */
    fun mutate(converter: (T) -> T): RINT

    /**
     * We close down underlying scope and close the drivers. You will need to create a new Driver and MultiTrack object
     * after calling this.
     */
    fun close()

    /**
     * Converts objects into binary streams and out of binary streams.
     *
     * @throws RuntimeException if it fails to convert for some reason.
     */
    interface Converter<T> {
        /**
         * These methods are atomic and will run during a queue operation, it is incorrect to do any sort of IO other
         * than converting to/from objects.
         *
         * @param source to read the stream from, it's not guaranteed the underlying source if buffered so depending on
         * your implementation it is advised to do that. The source is also closed on return from this method
         * @return the object from the source, if you fail to convert then return some form of [RuntimeException]
         * this will get passed up to the caller of the [MultiTrack] operation.
         */
        @Throws(RuntimeException::class)
        fun toValue(source: BufferedSource): T

        /**
         * These methods are atomic and will run during a queue operation, it is incorrect to do any sort of IO other
         * than converting to/from objects.
         *
         * @param value the object to convert to binary
         * @param sink where to write the object binary representation too (i.e. Object -> Json)
         */
        @Throws(RuntimeException::class)
        fun fromValue(value: T, sink: BufferedSink)
    }

    interface Driver : Closeable {

        /**
         * Write the blob to the the backing queue
         */
        fun write(blob: ByteArray): Boolean

        /**
         * Read the size of the queue
         *
         * @return current size of the queue
         */
        fun count(): Int

        /**
         * Peek for [limit] size
         *
         * @param limit null will try and return everything, if you know you have a large list, I would recommend you
         * set a limit
         *
         * @return will be empty if the list is empty
         */
        fun peek(limit: Int? = null): List<ByteArray>

        /**
         * Read and remove the item at the front of the queue.
         *
         * @return null if queue is empty
         */
        fun poll(): ByteArray?

        /**
         * Remove head of the queue
         * @param limit how many rows to delete, null is all of them
         * @return count of how many entries removed
         */
        fun remove(limit: Int? = null): Int

        /**
         * Pass in the bytearray to mutate then if the result is different save that back to the queue.
         *
         * @return the count of how many rows were mutated.
         */
        fun mutate(mapper: (byteArray: ByteArray) -> ByteArray): Int

        companion object Schema {
            private const val TABLE = "`multi_track`"
            const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE;"
            const val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS $TABLE (
                `id`      INTEGER PRIMARY KEY AUTOINCREMENT,
                `blob`    BLOB,
                `version` TEXT NOT NULL DEFAULT '0'
            );
            """
            const val INSERT = "INSERT INTO $TABLE (`blob`) VALUES (?)"
            const val COUNT = "SELECT COUNT(`id`) FROM $TABLE;"
            const val FETCH = "SELECT `id`, `blob`, `version` FROM $TABLE ORDER BY `id` LIMIT ?"
            const val UPDATE = "UPDATE $TABLE SET `blob` = ? WHERE `id` = ?"
            const val DELETE_BY_ID = "DELETE FROM $TABLE WHERE `id` = ?"
            const val DELETE_BY_LIMIT =
                "DELETE FROM $TABLE WHERE `id` IN (SELECT `id` FROM $TABLE ORDER BY `id` LIMIT ?);"
        }
    }
}
