package com.chrisjenx.multitrack.driver.android

import com.chrisjenx.multitrack.MultiTrack.Driver
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.COUNT
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.CREATE_TABLE
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.DELETE_BY_ID
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.DELETE_BY_LIMIT
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.FETCH
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.INSERT
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema.UPDATE
import com.chrisjenx.multitrack.driver.android.JdbcSqliteDriver.Companion.IN_MEMORY
import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.Properties

class JdbcSqliteDriver
internal constructor(
    url: String,
    properties: Properties = Properties()
) : Driver, Closeable {

    private val statements: MutableMap<String, PreparedStatement> = mutableMapOf()
    private val connection: Connection by lazy {
        DriverManager.getConnection(url, properties).also {
            it.prepareStatement(CREATE_TABLE).execute()
        }
    }

    override fun write(blob: ByteArray): Boolean {
        return connection.prepareStatement(INSERT).let {
            it.setBytes(1, blob)
            it.executeUpdate() == 1
        }
    }

    override fun count(): Int {
        val s = statements.getOrPut(COUNT) { connection.prepareStatement(COUNT) }
        val results = s.executeQuery()
        return results.getInt(1)
    }

    override fun peek(limit: Int?): List<ByteArray> {
        val s = statements.getOrPut(FETCH) { connection.prepareStatement(FETCH) }
        s.clearParameters()
        s.setLong(1, limit?.toLong() ?: Long.MAX_VALUE)
        val results = s.executeQuery()
        return generateSequence {
            if (!results.next()) return@generateSequence null
            results.getBytes(2)
        }.toList()
    }

    override fun poll(): ByteArray? {
        val s = statements.getOrPut(FETCH) { connection.prepareStatement(FETCH) }
        s.clearParameters()
        s.setLong(1, 1)
        val results = s.executeQuery()
        if (!results.next()) return null
        val id = results.getLong(1)
        val bytes = results.getBytes(2)
        val d = statements.getOrPut(DELETE_BY_ID) { connection.prepareStatement(DELETE_BY_ID) }
        d.clearParameters()
        d.setLong(1, id)
        if (d.executeUpdate() != 1) return null
        return bytes
    }

    override fun remove(limit: Int?): Int {
        val d = statements.getOrPut(DELETE_BY_LIMIT) { connection.prepareStatement(DELETE_BY_LIMIT) }
        d.clearParameters()
        d.setLong(1, limit?.toLong() ?: Long.MAX_VALUE)
        return d.executeUpdate()
    }

    override fun mutate(mapper: (byteArray: ByteArray) -> ByteArray): Int {
        // read all
        val fetch = statements.getOrPut(FETCH) { connection.prepareStatement(FETCH) }
        val update = statements.getOrPut(UPDATE) { connection.prepareStatement(UPDATE) }
        fetch.clearParameters()
        fetch.setLong(1, Long.MAX_VALUE)
        val results = fetch.executeQuery()
        // Generate sequence and return count maybe ?
        update.clearParameters()
        while (results.next()) {
            val id = results.getLong(1)
            val blob = results.getBytes(2)
            update.setBytes(1, mapper(blob))
            update.setLong(2, id)
            update.addBatch()
        }
        // Add the total of updated rows
        return update.executeBatch().reduce { acc, i -> acc + i }
    }

    override fun close() {
        connection.close()
    }

    companion object {
        const val IN_MEMORY = "jdbc:sqlite:"
    }
}

/**
 * Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
 * (creating an in-memory database) or a path to a file.
 */
fun SqliteDriver(
    url: String,
    properties: Properties = Properties()
): JdbcSqliteDriver {
    check(url.isNotBlank()) { "Provide a jdbc:sqlite:path" }
    return JdbcSqliteDriver(url, properties)
}

/**
 * Create an in memory Sqlite db connection.
 */
fun SqlightDriver(properties: Properties): JdbcSqliteDriver {
    return JdbcSqliteDriver(IN_MEMORY, properties)
}
