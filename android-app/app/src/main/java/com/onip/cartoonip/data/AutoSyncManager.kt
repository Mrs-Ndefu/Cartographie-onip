package com.onip.cartoonip.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Relance automatiquement la synchronisation des ménages en attente dès que l'appareil retrouve
 * une connexion internet validée ET qu'un agent est connecté. L'agent n'a plus besoin d'ouvrir
 * le Journal pour ça ; la synchro manuelle (icônes de relance, "Tout synchroniser") reste
 * disponible en complément pour les cas où l'auto-sync a échoué (ex : serveur down).
 */
object AutoSyncManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private val networkAvailable = MutableStateFlow(false)
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true

        val connectivityManager = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // NET_CAPABILITY_VALIDATED n'existe qu'à partir de l'API 23 — sur la tablette terrain
        // (API 21) on se contente de NET_CAPABILITY_INTERNET, sans attendre la validation réseau.
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                }
            }
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                networkAvailable.value = true
            }

            override fun onLost(network: Network) {
                networkAvailable.value = false
            }
        })

        scope.launch {
            combine(networkAvailable, AppContainer.sessionManager.session) { online, session -> online && session != null }
                .distinctUntilChanged()
                .collect { readyToSync -> if (readyToSync) syncPending() }
        }
    }

    private suspend fun syncPending() {
        if (!syncMutex.tryLock()) return
        try {
            val pending = AppContainer.captureStore.households.value.filterNot { it.isFullySynced }
            pending.forEach { household ->
                runCatching { SyncRepository.sync(household) }
            }
        } finally {
            syncMutex.unlock()
        }
    }
}
