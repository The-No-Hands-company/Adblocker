package com.tnhc.shield.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tnhc.shield.data.prefs.ShieldPreferences
import com.tnhc.shield.vpn.ShieldVpnService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var prefs: ShieldPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.IO).launch {
            val autoStart = prefs.autoStart.first()
            if (autoStart) {
                val vpnIntent = Intent(context, ShieldVpnService::class.java)
                    .setAction(ShieldVpnService.ACTION_START)
                context.startForegroundService(vpnIntent)
            }
        }
    }
}
