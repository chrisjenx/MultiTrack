package com.chrisjenx.multitrack.driver.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chrisjenx.multitrack.BaseDriverTest
import com.chrisjenx.multitrack.MultiTrack
import com.google.common.truth.Truth.assertThat
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSqliteDriverTest : BaseDriverTest() {

    private val driver = AndroidSqliteDriver(ApplicationProvider.getApplicationContext<Context>())

    override fun createDriver(): MultiTrack.Driver {
        return driver
    }

}