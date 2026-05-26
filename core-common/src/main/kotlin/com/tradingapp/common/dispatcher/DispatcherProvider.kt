package com.tradingapp.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Abstracts coroutine dispatchers to allow test injection of [TestCoroutineDispatcher]
 * without hard-coding [Dispatchers.IO] etc. throughout the codebase.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}
