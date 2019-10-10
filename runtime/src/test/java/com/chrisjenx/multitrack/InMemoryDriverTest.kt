package com.chrisjenx.multitrack

class InMemoryDriverTest : BaseDriverTest() {

    override fun createDriver(): MultiTrack.Driver {
        return InMemoryDriver()
    }
}
