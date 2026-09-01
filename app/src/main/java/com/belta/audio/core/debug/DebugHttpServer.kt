package com.belta.audio.core.debug

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

/**
 * Embedded lightweight HTTP server on port 8080 for real-time PC log streaming.
 * Allows developers to open http://<device-ip>:8080 in their PC browser to monitor live logs.
 */
class DebugHttpServer(
    private val context: Context,
    private val scope: CoroutineScope,
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    fun start() {
        if (serverJob?.isActive == true) return
        serverJob = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(port)
                DebugLogger.i(
                    LogCategory.SYSTEM,
                    "HTTP_SERVER",
                    "PC Real-Time Debug Server running at http://${getDeviceIpAddress()}:$port"
                )

                while (isActive) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket)
                        }
                    } catch (_: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                DebugLogger.w(
                    LogCategory.SYSTEM,
                    "HTTP_SERVER",
                    "Could not start HTTP server on port $port: ${e.localizedMessage}"
                )
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
            serverSocket = null
        } catch (_: Exception) {}
        serverJob?.cancel()
        serverJob = null
    }

    fun getDeviceIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
            if (ipInt != 0) {
                String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            } else {
                "127.0.0.1"
            }
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    val serverUrl: String
        get() = "http://${getDeviceIpAddress()}:$port"

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()
            val requestLine = reader.readLine() ?: return

            val tokens = requestLine.split(" ")
            val method = if (tokens.isNotEmpty()) tokens[0] else "GET"
            val path = if (tokens.size > 1) tokens[1] else "/"

            when {
                path == "/api/logs" -> {
                    val logsJson = buildLogsJson()
                    sendJsonResponse(out, logsJson)
                }
                path == "/api/clear" -> {
                    DebugLogger.clearLogs()
                    sendJsonResponse(out, "{\"status\":\"cleared\"}")
                }
                else -> {
                    val html = buildWebConsoleHtml()
                    sendHtmlResponse(out, html)
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {}
        }
    }

    private fun sendHtmlResponse(out: OutputStream, html: String) {
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun sendJsonResponse(out: OutputStream, json: String) {
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val header = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Connection: close\r\n\r\n"
        out.write(header.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun buildLogsJson(): String {
        val list = DebugLogger.logs.value
        val sb = StringBuilder("[")
        list.forEachIndexed { index, log ->
            sb.append("{")
            sb.append("\"id\":${log.id},")
            sb.append("\"time\":\"${log.formattedTime}\",")
            sb.append("\"level\":\"${log.level.name}\",")
            sb.append("\"category\":\"${log.category.name}\",")
            sb.append("\"tag\":\"${escapeJson(log.tag)}\",")
            sb.append("\"message\":\"${escapeJson(log.message)}\",")
            sb.append("\"details\":${if (log.details != null) "\"${escapeJson(log.details)}\"" else "null"}")
            sb.append("}")
            if (index < list.size - 1) sb.append(",")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun buildWebConsoleHtml(): String {
        val hw = DebugLogger.getAudioHardwareInfo(context)
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Belta Audio - Real-Time PC Live Log Console</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace; }
        body { background: #0b0e14; color: #e6edf3; padding: 20px; }
        .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #30363d; padding-bottom: 14px; margin-bottom: 16px; }
        h1 { font-size: 20px; color: #58a6ff; }
        .hw-card { background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 14px; margin-bottom: 16px; display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 10px; font-size: 13px; }
        .hw-item { color: #8b949e; }
        .hw-val { color: #58a6ff; font-weight: bold; }
        .controls { display: flex; gap: 10px; margin-bottom: 14px; flex-wrap: wrap; align-items: center; }
        button { background: #21262d; color: #c9d1d9; border: 1px solid #30363d; padding: 6px 14px; border-radius: 6px; cursor: pointer; font-size: 13px; }
        button:hover { background: #30363d; }
        button.active { background: #1f6feb; color: #fff; border-color: #388bfd; }
        #log-container { background: #010409; border: 1px solid #30363d; border-radius: 8px; height: calc(100vh - 240px); overflow-y: auto; padding: 12px; font-family: Consolas, 'Courier New', monospace; font-size: 12px; }
        .log-row { display: flex; gap: 10px; padding: 3px 0; border-bottom: 1px solid #161b22; }
        .log-time { color: #8b949e; width: 85px; flex-shrink: 0; }
        .log-level-INFO { color: #3fb950; font-weight: bold; width: 25px; }
        .log-level-WARN { color: #d29922; font-weight: bold; width: 25px; }
        .log-level-ERROR { color: #f85149; font-weight: bold; width: 25px; }
        .log-level-DEBUG { color: #58a6ff; font-weight: bold; width: 25px; }
        .log-category { color: #00f0ff; width: 130px; flex-shrink: 0; font-weight: 600; }
        .log-tag { color: #ffa657; width: 130px; flex-shrink: 0; }
        .log-msg { color: #e6edf3; flex-grow: 1; word-break: break-all; }
        .log-details { color: #a5d6ff; font-size: 11px; margin-top: 2px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Belta Audio - Live PC Debug Console</h1>
        <div>Live Stream: <span id="status" style="color: #3fb950; font-weight: bold;">CONNECTED</span></div>
    </div>
    <div class="hw-card">
        <div><span class="hw-item">Device: </span><span class="hw-val">${hw.deviceModel}</span></div>
        <div><span class="hw-item">OS: </span><span class="hw-val">${hw.androidVersion}</span></div>
        <div><span class="hw-item">Native Rate/Buffer: </span><span class="hw-val">${hw.nativeSampleRate} / ${hw.nativeBufferSize}</span></div>
        <div><span class="hw-item">Low Latency / Pro: </span><span class="hw-val">${if (hw.hasLowLatencyAudio) "Supported" else "No"} / ${if (hw.hasProAudio) "Pro" else "Standard"}</span></div>
    </div>
    <div class="controls">
        <button id="btn-pause" onclick="togglePause()">Pause Stream</button>
        <button onclick="clearLogs()">Clear Console</button>
        <button id="btn-autoscroll" class="active" onclick="toggleAutoScroll()">Auto-Scroll: ON</button>
        <span style="color: #8b949e; margin-left: 10px;">Total Logs: <span id="log-count" style="color: #fff;">0</span></span>
    </div>
    <div id="log-container"></div>

    <script>
        let isPaused = false;
        let autoScroll = true;
        let lastLogId = 0;

        function togglePause() {
            isPaused = !isPaused;
            document.getElementById('btn-pause').innerText = isPaused ? 'Resume Stream' : 'Pause Stream';
            document.getElementById('btn-pause').className = isPaused ? 'active' : '';
        }

        function toggleAutoScroll() {
            autoScroll = !autoScroll;
            document.getElementById('btn-autoscroll').innerText = 'Auto-Scroll: ' + (autoScroll ? 'ON' : 'OFF');
            document.getElementById('btn-autoscroll').className = autoScroll ? 'active' : '';
        }

        function clearLogs() {
            fetch('/api/clear').then(() => {
                document.getElementById('log-container').innerHTML = '';
                document.getElementById('log-count').innerText = '0';
                lastLogId = 0;
            });
        }

        function pollLogs() {
            if (!isPaused) {
                fetch('/api/logs')
                    .then(res => res.json())
                    .then(logs => {
                        document.getElementById('log-count').innerText = logs.length;
                        const container = document.getElementById('log-container');
                        const newLogs = logs.filter(l => l.id > lastLogId);
                        if (newLogs.length > 0) {
                            newLogs.forEach(l => {
                                const row = document.createElement('div');
                                row.className = 'log-row';
                                row.innerHTML = `
                                    <div class="log-time">${'$'}{l.time}</div>
                                    <div class="log-level-${'$'}{l.level}">[${'$'}{l.level[0]}]</div>
                                    <div class="log-category">${'$'}{l.category}</div>
                                    <div class="log-tag">${'$'}{l.tag}</div>
                                    <div class="log-msg">
                                        ${'$'}{l.message}
                                        ${'$'}{l.details ? '<div class="log-details">Details: ' + l.details + '</div>' : ''}
                                    </div>
                                `;
                                container.appendChild(row);
                                lastLogId = l.id;
                            });
                            if (autoScroll) {
                                container.scrollTop = container.scrollHeight;
                            }
                        }
                    })
                    .catch(() => {
                        document.getElementById('status').innerText = 'RECONNECTING...';
                        document.getElementById('status').style.color = '#f85149';
                    });
            }
        }

        setInterval(pollLogs, 600);
    </script>
</body>
</html>
        """.trimIndent()
    }
}
