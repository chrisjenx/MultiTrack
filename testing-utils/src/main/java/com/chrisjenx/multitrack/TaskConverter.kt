package com.chrisjenx.multitrack

import okio.BufferedSink
import okio.BufferedSource
import java.io.IOException

class TaskConverter : MultiTrack.Converter<Task> {

    var throwError = false

    override fun toValue(source: BufferedSource): Task {
        if (throwError) throw IOException("Failed to read from buffer")
        return Task(source.readInt())
    }

    override fun fromValue(value: Task, sink: BufferedSink) {
        if (throwError) throw IOException("Failed to write from buffer")
        sink.writeInt(value.id)
    }
}