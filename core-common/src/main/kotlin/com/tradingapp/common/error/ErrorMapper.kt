package com.tradingapp.common.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    fun toUserMessage(throwable: Throwable): String = when (throwable) {
        is UnknownHostException -> "No internet connection. Showing cached data."
        is SocketTimeoutException -> "Request timed out. Please retry."
        is IOException -> "Connection error. Please retry."
        else -> throwable.localizedMessage?.takeIf { it.isNotBlank() }
            ?: "An unexpected error occurred."
    }
}
