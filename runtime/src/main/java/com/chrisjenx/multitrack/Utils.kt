package com.chrisjenx.multitrack

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Catches any ex in the [block] and will pass it to the receiver [CompletableDeferred.completeExceptionally] method
 * passing the error upstream instead of failing here.
 */
internal inline fun <T> CompletableDeferred<T>.deferEx(
    invokeOnComplete: (T) -> Unit = {},
    block: () -> T
): CompletableDeferred<T> {
    try {
        val result = block()
        complete(result)
        invokeOnComplete(result)
    } catch (e: Exception) {
        completeExceptionally(e)
    }
    return this
}

internal fun <T> CoroutineScope.launchPropEx(
    deferred: CompletableDeferred<T>, block: suspend CoroutineScope.() -> Unit
) {
    launch(
        context = CoroutineExceptionHandler { _, throwable -> deferred.completeExceptionally(throwable) },
        block = block
    )
}
