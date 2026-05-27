package com.tradingapp.common.network

import kotlinx.coroutines.flow.Flow

/** App-wide source of truth for network connectivity. Pure Kotlin interface — no Android deps. */
interface NetworkMonitor {
    /** Emits true when a network with Internet capability is available, false when lost. */
    fun observeIsOnline(): Flow<Boolean>
}
