# MultiTrack

[![CircleCI](https://circleci.com/gh/chrisjenx/MultiTrack/tree/master.svg?style=svg)](https://circleci.com/gh/chrisjenx/MultiTrack/tree/master)
[![JitPack](https://jitpack.io/v/com.chrisjenx/MultiTrack.svg)](https://jitpack.io/#com.chrisjenx/MultiTrack)

MultiTrack is designed as a replacement for [Tape](https://github.com/square/tape).
Instead of a File Queue we use an SQLite backed queue and is backed by Kotlin Coroutines to provide non-blocking
transactions.

## Creating the Queue

Pick which Driver you want to create (we support InMemoryDriver/SqliteDriver/AndroidSqliteDriver).

```kotlin
val driver = AndroidSqliteDriver(applicationContext, "multitrack-database")
```

Create the Converter which is responsible for parsing your objects into binary - we recommend [Moshi](https://github.com/square/moshi)

```kotlin
@JsonClass(generateAdapter = true)
data class QueueObject(val id: Int)

val moshi = //.. create moshi instance
val converter = object : MultiTrack.Converter<QueueObject> {
    val adapter = moshi.adapter(QueueObject::class.java)
    override fun toValue(source: BufferedSource): QueueObject {
        return adapter.fromJson(source)
    }

    override fun fromValue(value: QueueObject, sink: BufferedSink) {
        adapter.toJson(sink, value)
    }
}
```

You can pick Coroutine/Callback/RxJava2 interfaces to the queue - note Driver and MultiTrack should be unique once
per application.

RxJava2:

```kotlin
val multiTrack = MultiTrack(Schedulers.io(), driver, converter)
```
Coroutine:

```kotlin
val multiTrack = MultiTrack(Dispatchers.IO, driver, converter)
```

Java:
```kotlin
val multiTrack = MultiTrack(Executors.newSingleThreadScheduledExecutor(), driver, converter)
```

With all of these, because the underlying implementation is actor based - it's acceptable to pass in a single thread.
However it's not recommended that you use that same single thread after getting a result as you would then be suspending
MultiTrack while you do work.


## Read/Write

The Queue acts like any other Queue style implementation you _push_ onto the end (tail) of the queue and _poll_ from
the front (head) of the queue.

```kotlin
// Write to the queue
multiTrack.push(QueueObject(10)).await()

// 1 Object added to the queue
assert(multiTrack.size().await() == 1) // True

// We look ahead on the queue (peek) - leave the item on the queue.
assert(multiTrack.peek().await().id == 10) // True

// Then read and remove the head of the queue
assert(multiTrack.poll().await().id == 10) // True

// 1 Object added to the queue
assert(multiTrack.size().await() == 0) // True
```

There is also convenience `sizeStream()` for getting queue size callbacks. We recommend using RxJava2 or Coroutine Flow
versions, while there is a Java implementation - the others have much more complete streaming api's.

## Dependencies

Default/Kotlin:
 - InMemoryDriver
 - DefaultMultiTrack
 - CoroutineMultiTrack

```groovy
implementation "com.chrisjenx.multitrack:runtime:$release"
```

RxJava:
 - RxJava2MultiTrack

```groovy
implementation "com.chrisjenx.multitrack:rxjava2:$release"

```

Java Sqlite:
 - SqliteDriver

```groovy
implementation "com.chrisjenx.multitrack:driver-sqlite:$release"

```

Java Sqlite (aar binary):
 - AndroidSqliteDriver

```groovy
implementation "com.chrisjenx.multitrack:driver-android:$release"

```
