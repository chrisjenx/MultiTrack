@file:Suppress("FunctionName")

package com.chrisjenx.multitrack.driver.android

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteStatement
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.chrisjenx.multitrack.MultiTrack
import com.chrisjenx.multitrack.MultiTrack.Driver.Schema
import java.io.Closeable

class AndroidSqliteDriver
internal constructor(
    private val openHelper: SupportSQLiteOpenHelper? = null,
    database: SupportSQLiteDatabase? = null
) : MultiTrack.Driver, Closeable {

    init {
        require((openHelper != null) xor (database != null))
    }

    private val statements: MutableMap<String, SupportSQLiteStatement> = mutableMapOf()
    private val database by lazy {
        (openHelper?.writableDatabase ?: database!!)
    }

    override fun write(blob: ByteArray): Boolean {
        val stmt = statements.getOrPut(Schema.INSERT) { database.compileStatement(Schema.INSERT) }
        stmt.clearBindings()
        stmt.bindBlob(1, blob)
        return stmt.executeInsert() == 1L
    }

    override fun count(): Int {
        val stmt = statements.getOrPut(Schema.COUNT) { database.compileStatement(Schema.COUNT) }
        return stmt.simpleQueryForLong().toInt()
    }

    override fun peek(limit: Int?): List<ByteArray> {
        val results = database.query(Schema.FETCH, arrayOf(limit?.toLong() ?: Long.MAX_VALUE))
        return generateSequence {
            if (!results.moveToNext()) return@generateSequence null
            results.getBlob(1)
        }.toList()
    }

    override fun poll(): ByteArray? = transaction {
        val results = database.query(Schema.FETCH, arrayOf(1))
        if (!results.moveToNext()) return@transaction null
        val id = results.getLong(0)
        val bytes = results.getBlob(1)
        val stmt = statements.getOrPut(Schema.DELETE_BY_ID) { database.compileStatement(Schema.DELETE_BY_ID) }
        stmt.clearBindings()
        stmt.bindLong(1, id)
        if (stmt.executeUpdateDelete() != 1) return@transaction null
        return@transaction bytes
    }

    override fun remove(limit: Int?): Int {
        val stmt = statements.getOrPut(Schema.DELETE_BY_LIMIT) { database.compileStatement(Schema.DELETE_BY_LIMIT) }
        stmt.clearBindings()
        stmt.bindLong(1, limit?.toLong() ?: Long.MAX_VALUE)
        return stmt.executeUpdateDelete()
    }

    override fun mutate(mapper: (byteArray: ByteArray) -> ByteArray): Int = transaction {
        val fetch = database.query(Schema.FETCH, arrayOf(Long.MAX_VALUE))
        val update = statements.getOrPut(Schema.UPDATE) { database.compileStatement(Schema.UPDATE) }
        val results = generateSequence {
            if (!fetch.moveToNext()) return@generateSequence null
            fetch.getLong(0) to fetch.getBlob(1)
        }
        results
            .map {
                update.clearBindings()
                update.bindBlob(1, mapper(it.second))
                update.bindLong(2, it.first)
                update.executeUpdateDelete()
            }
            .reduce { acc, i -> acc + i }
    }

    private fun <T> transaction(block: SupportSQLiteDatabase.() -> T): T {
        database.beginTransaction()
        val result: T
        try {
            result = database.block()
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        return result
    }

    override fun close() {
        database.close()
    }

    internal class Callback : SupportSQLiteOpenHelper.Callback(1) {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(Schema.CREATE_TABLE)
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(Schema.DROP_TABLE)
            db.execSQL(Schema.CREATE_TABLE)
        }
    }
}

fun AndroidSqliteDriver(openHelper: SupportSQLiteOpenHelper): AndroidSqliteDriver {
    return AndroidSqliteDriver(openHelper = openHelper, database = null)
}

fun AndroidSqliteDriver(database: SupportSQLiteDatabase): AndroidSqliteDriver {
    return AndroidSqliteDriver(openHelper = null, database = database)
}

/**
 * AndroidDriver Factory creator, this is the easiest way to create the driver, you pass in context and db name.
 *
 * @param context Application Context
 * @param name Name of the Database file (should be constant between runs for persistance). Null will create "in-memory"
 * database.
 * @param factory Helper Factory for creating databases on android - only override if you know that you need to.
 */
fun AndroidSqliteDriver(
    context: Context,
    name: String? = null,
    factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory()
): AndroidSqliteDriver {
    return AndroidSqliteDriver(
        openHelper = factory.create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .callback(AndroidSqliteDriver.Callback())
                .name(name)
                .build()
        ),
        database = null
    )
}