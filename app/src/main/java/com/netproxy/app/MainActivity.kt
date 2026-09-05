package com.netproxy.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        var proxyServer: LocalProxyServer? = null
        var isServerRunning: Boolean = false
        var activePort: Int = 8080
        private val handler = Handler(Looper.getMainLooper())
        private var shutdownRunnable: Runnable? = null

        fun stopProxyServerGlobal() {
            try {
                proxyServer?.stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            proxyServer = null
            isServerRunning = false
            shutdownRunnable?.let { handler.removeCallbacks(it) }
            shutdownRunnable = null
        }

        fun startProxyServerGlobal(context: android.content.Context, port: Int, password: String, minutes: Int, onStatusChanged: ((Boolean, Int) -> Unit)? = null) {
            try {
                if (proxyServer != null) {
                    stopProxyServerGlobal()
                }

                proxyServer = LocalProxyServer(port, password, context.applicationContext)
                proxyServer?.start()
                isServerRunning = true
                activePort = port
                onStatusChanged?.invoke(true, port)

                shutdownRunnable?.let { handler.removeCallbacks(it) }

                if (minutes > 0) {
                    val millis = minutes * 60 * 1000L
                    shutdownRunnable = Runnable {
                        stopProxyServerGlobal()
                        onStatusChanged?.invoke(false, port)
                    }
                    handler.postDelayed(shutdownRunnable!!, millis)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                proxyServer = null
                isServerRunning = false
                onStatusChanged?.invoke(false, port)
            }
        }
    }

    private lateinit var etPort: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTurnOn: Button
    private lateinit var btnTurnOff: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etPort = findViewById(R.id.etPort)
        etPassword = findViewById(R.id.etPassword)
        btnTurnOn = findViewById(R.id.btnTurnOn)
        btnTurnOff = findViewById(R.id.btnTurnOff)
        tvStatus = findViewById(R.id.tvStatus)

        updateStatusUI(isServerRunning, activePort)

        btnTurnOn.setOnClickListener {
            startProxyServerManual()
        }

        btnTurnOff.setOnClickListener {
            stopProxyServerManual()
        }

        handleNetProxyIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            setIntent(intent)
            handleNetProxyIntent(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusUI(isServerRunning, activePort)
    }

    private fun handleNetProxyIntent(intent: Intent) {
        if (Intent.ACTION_VIEW == intent.action) {
            val uri: Uri? = intent.data
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

                val portStr = etPort.text.toString().trim()
                val passwordStr = etPassword.text.toString().trim()
                val port = if (portStr.isNotEmpty()) portStr.toInt() else 8080
                val password = if (passwordStr.isNotEmpty()) passwordStr else "7777"

                startProxyServerGlobal(this, port, password, minutes) { running, p ->
                    runOnUiThread { updateStatusUI(running, p) }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        finish()
                        moveTaskToBack(true)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 500)
            }
        }
    }

    private fun startProxyServerManual() {
        val portStr = etPort.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val port = if (portStr.isNotEmpty()) portStr.toInt() else 8080
        val passwordFinal = if (password.isNotEmpty()) password else "7777"

        startProxyServerGlobal(this, port, passwordFinal, 0) { running, p ->
            runOnUiThread { updateStatusUI(running, p) }
        }
        updateStatusUI(true, port)
    }

    private fun stopProxyServerManual() {
        stopProxyServerGlobal()
        updateStatusUI(false)
    }

    private fun updateStatusUI(isRunning: Boolean, port: Int = 8080) {
        if (isRunning) {
            tvStatus.text = "Status: RUNNING (127.0.0.1:$port)"
            tvStatus.setTextColor(Color.parseColor("#1DB954"))
            etPort.isEnabled = false
            etPassword.isEnabled = false
        } else {
            tvStatus.text = "Status: OFF"
            tvStatus.setTextColor(Color.parseColor("#E50914"))
            etPort.isEnabled = true
            etPassword.isEnabled = true
        }
    }
}
