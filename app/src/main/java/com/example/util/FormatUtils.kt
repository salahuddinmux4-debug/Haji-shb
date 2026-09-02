package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatPkr(amount: Double, showDecimalsIfAny: Boolean = false): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        return if (showDecimalsIfAny && amount % 1.0 != 0.0) {
            formatter.minimumFractionDigits = 2
            formatter.maximumFractionDigits = 2
            "Rs. ${formatter.format(amount)}"
        } else {
            formatter.minimumFractionDigits = 0
            formatter.maximumFractionDigits = 0
            "Rs. ${formatter.format(amount)}"
        }
    }

    fun formatDateOnly(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatTimeOnly(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

class NetworkMonitor(private val context: Context) {
    val isOnlineFlow: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        fun checkOnline(): Boolean {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

        trySend(checkOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}
