package com.vipmail.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.vipmail.ui.theme.VIPMailTheme
import com.vipmail.worker.ImapSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scheduleSync()
        requestNotificationPermission()
        setContent {
            VIPMailTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainNavigation()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATIONS
                )
            }
        }
    }

    private fun scheduleSync() {
        val request = PeriodicWorkRequestBuilder<ImapSyncWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "imap_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1001
    }
}

