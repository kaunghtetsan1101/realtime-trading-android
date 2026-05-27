package com.tradingapp.common.result

/**
 * A generic wrapper for UI-layer results. Replaces raw exceptions crossing layer boundaries.
 *
 * Tradeoff: We keep this separate from Kotlin's built-in [kotlin.Result] to:
 * - Carry a meaningful [message] for display purposes in the Loading state.
 * - Avoid confusion with stdlib's sealed interface (different API surface).
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()

    data class Error(val exception: Throwable, val message: String = exception.localizedMessage ?: "Unknown error") :
        Result<Nothing>()

    data object Loading : Result<Nothing>()
}

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Result.Error) -> Unit): Result<T> {
    if (this is Result.Error) action(this)
    return this
}

inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) action()
    return this
}

fun <T> Result<T>.getOrNull(): T? = if (this is Result.Success) data else null
