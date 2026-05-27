package com.tradingapp.domain.usecase

import com.tradingapp.common.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Exposes app-wide network status to the feature layer.
 * Thin delegation so ViewModels do not depend directly on core-network internals.
 */
class ObserveNetworkStatusUseCase
@Inject
constructor(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.observeIsOnline()
}
