package com.mazkiplay.trade

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mazkiplay.trade.data.repository.UserPreferences
import com.mazkiplay.trade.service.MarketRefreshWorker
import com.mazkiplay.trade.ui.MazkiplayNavHost
import com.mazkiplay.trade.ui.theme.MazkiplayTheme

/**
 * Single-activity host.
 *
 * The activity owns three platform concerns that do not belong in Compose:
 * the notification permission request, the screen-brightness preference, and
 * scheduling of the background refresh worker.
 */
class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The app degrades gracefully when denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val app = application as MazkiplayApp

        requestNotificationPermissionIfNeeded()
        MarketRefreshWorker.schedule(this)

        setContent {
            val prefs by app.settings.preferences.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )
            val prefsSnapshot by app.settings.preferences.collectAsState(initial = UserPreferences())

            // Apply the chosen screen brightness for as long as this activity lives.
            LaunchedEffect(prefsSnapshot.brightness) {
                applyBrightness(prefsSnapshot.brightness)
            }

            MazkiplayTheme(themeMode = prefs.theme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MazkiplayNavHost(app = app)
                }
            }
        }
    }

    private fun applyBrightness(value: Float) {
        val params = window.attributes
        params.screenBrightness = value.coerceIn(0.05f, 1.0f)
        window.attributes = params
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.takeIf { value >= 1.0f } ?: 0)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
