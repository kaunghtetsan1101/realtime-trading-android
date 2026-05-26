package com.tradingapp.common.extension

import com.tradingapp.common.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Wraps a [Flow]<T> into a [Flow]<Result<T>>, emitting [Result.Loading] first,
 * then [Result.Success] for each item, and [Result.Error] on any exception.
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> = this
    .map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }
