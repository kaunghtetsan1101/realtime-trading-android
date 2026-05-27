package com.tradingapp.network.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.tradingapp.common.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitorImpl
@Inject
constructor(@ApplicationContext private val context: Context) : NetworkMonitor {
    override fun observeIsOnline(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

        // Track all networks currently satisfying the request. A device can have multiple
        // active networks (e.g. WiFi + cellular). Emitting false on any single onLost() would
        // be wrong if another network is still available, so we track the full set.
        val availableNetworks: MutableSet<Network> = Collections.synchronizedSet(mutableSetOf())

        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    availableNetworks += network
                    trySend(availableNetworks.isNotEmpty())
                }

                override fun onLost(network: Network) {
                    availableNetworks -= network
                    trySend(availableNetworks.isNotEmpty())
                }
            }

        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit current state immediately so collectors do not wait for the next change event.
        val isCurrentlyOnline =
            connectivityManager.activeNetwork
                ?.let { connectivityManager.getNetworkCapabilities(it) }
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(isCurrentlyOnline)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}
