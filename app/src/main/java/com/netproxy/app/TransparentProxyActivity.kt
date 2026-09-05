package com.netproxy.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TransparentProxyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            handleIntent(intent)
        }
    }

    private fun handleIntent(incomingIntent: Intent) {
        if (Intent.ACTION_VIEW == incomingIntent.action) {
            val uri = incomingIntent.data
            if (uri != null && uri.scheme == "netproxy") {
                var startParam = uri.getQueryParameter("start")

                if (startParam == null) {
                    val hostOrPath = uri.host ?: uri.schemeSpecificPart
                    if (hostOrPath != null && hostOrPath.contains("start=")) {
                        val parts = hostOrPath.replace("//", "").split("=")
                        if (parts.size > 1) {
                            startParam = parts[1]
                        }
                    }
                }

                val minutes = startParam?.toIntOrNull() ?: 5
                val port = 8080
                val password = "7777"

                MainActivity.startProxyServerGlobal(this, port, password, minutes)
            }
        }
        finish()
    }
}
