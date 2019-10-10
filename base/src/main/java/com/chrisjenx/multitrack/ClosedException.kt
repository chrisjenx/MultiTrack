package com.chrisjenx.multitrack

class ClosedException(
    msg: String = "The resource has been closed",
    cause: Throwable? = null
) : RuntimeException(msg, cause)