package com.chrisjenx.multitrack.driver.android

import com.chrisjenx.multitrack.BaseDriverTest
import com.chrisjenx.multitrack.MultiTrack

class JdbcSqliteDriverTest : BaseDriverTest() {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    override fun createDriver(): MultiTrack.Driver {
        return driver
    }
}