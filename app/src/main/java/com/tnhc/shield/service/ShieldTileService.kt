package com.tnhc.shield.service

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tnhc.shield.MainActivity
import com.tnhc.shield.vpn.ShieldVpnService
import kotlinx.coroutines.*

class ShieldTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        stateJob?.cancel()
        stateJob = serviceScope.launch {
            ShieldVpnService.isRunning.collect { running ->
                updateTileState(running)
            }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        stateJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = ShieldVpnService.isRunning.value

        if (isRunning) {
            // Stop VPN
            val stopIntent = Intent(this, ShieldVpnService::class.java)
                .setAction(ShieldVpnService.ACTION_STOP)
            startService(stopIntent)
            updateTileState(false)
        } else {
            // Check if VPN permission is granted
            val prepareIntent = VpnService.prepare(this)
            if (prepareIntent == null) {
                // Permission already granted, start directly
                val startIntent = Intent(this, ShieldVpnService::class.java)
                    .setAction(ShieldVpnService.ACTION_START)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(startIntent)
                } else {
                    startService(startIntent)
                }
                updateTileState(true)
            } else {
                // Must open activity to request VPN permission
                val openIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = PendingIntent.getActivity(
                        this,
                        0,
                        openIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(openIntent)
                }
            }
        }
    }

    private fun updateTileState(isActive: Boolean) {
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Shield"
        tile.subtitle = if (isActive) "Active" else "Off"
        tile.updateTile()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
