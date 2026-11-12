package com.tnhc.shield.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.tnhc.shield.MainActivity
import com.tnhc.shield.R
import com.tnhc.shield.data.prefs.ShieldPreferences
import com.tnhc.shield.dns.DnsEngine
import com.tnhc.shield.dns.DnsPacketParser
import com.tnhc.shield.dns.IpPacketParser
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.nio.ByteBuffer
import javax.inject.Inject

@AndroidEntryPoint
class ShieldVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.tnhc.shield.START_VPN"
        const val ACTION_STOP = "com.tnhc.shield.STOP_VPN"
        const val VPN_ADDRESS = "10.0.0.2"
        const val VPN_DNS = "10.0.0.2"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "shield_vpn"
    }

    @Inject lateinit var dnsEngine: DnsEngine
    @Inject lateinit var prefs: ShieldPreferences

    private var vpnInterface: ParcelFileDescriptor? = null
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopShield()
                return START_NOT_STICKY
            }
            else -> startShield()
        }
        return START_STICKY
    }

    private fun startShield() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        setupVpn()
    }

    private fun setupVpn() {
        serviceScope.launch {
            try {
                val excluded = prefs.excludedApps.first()
                val builder = Builder()
                    .addAddress(VPN_ADDRESS, 32)
                    .addRoute(VPN_ADDRESS, 32)
                    .addDnsServer(VPN_DNS)
                    .setSession("Shield")
                    .setMtu(1500)
                excluded.forEach { pkg ->
                    try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
                }
                vpnInterface = builder.establish()
                vpnInterface?.let { pfd ->
                    serviceJob = launch { runPacketLoop(pfd) }
                }
            } catch (e: Exception) {
                stopSelf()
            }
        }
    }

    private suspend fun runPacketLoop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteBuffer.allocate(32_768)

        while (currentCoroutineContext().isActive) {
            buffer.clear()
            val length = withContext(Dispatchers.IO) {
                try { input.read(buffer.array()) } catch (e: Exception) { -1 }
            }
            if (length <= 0) {
                delay(1)
                continue
            }

            val raw = buffer.array().copyOf(length)

            // Process packet asynchronously; write response back
            serviceScope.launch {
                val response = processPacket(raw)
                if (response != null) {
                    withContext(Dispatchers.IO) {
                        try { output.write(response) } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    private suspend fun processPacket(raw: ByteArray): ByteArray? {
        val ipUdp = IpPacketParser.parseUdpPacket(raw) ?: return null

        // Only handle DNS (port 53)
        if (ipUdp.dstPort != 53) return null

        val dnsPayload = ipUdp.payload
        val dnsResponse = dnsEngine.handleDnsQuery(dnsPayload)

        return IpPacketParser.buildUdpResponse(ipUdp, dnsResponse)
    }

    private fun stopShield() {
        serviceJob?.cancel()
        serviceJob = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceScope.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ShieldVpnService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_vpn_title))
            .setContentText(getString(R.string.notification_vpn_text))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_vpn),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_vpn_text)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
