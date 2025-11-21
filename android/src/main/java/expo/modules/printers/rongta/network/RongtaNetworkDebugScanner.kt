package expo.modules.printers.rongta.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

object RongtaNetworkDebugScanner {
    private const val TAG = "RongtaNetDebug"

    suspend fun debugNetworkSetup(context: Context) = withContext(Dispatchers.IO) {
        try {
            // Check WiFi state
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            Log.d(TAG, "WiFi connected: ${wifiManager?.isWifiEnabled}")
            Log.d(TAG, "WiFi SSID: ${wifiInfo?.ssid}")
            Log.d(TAG, "WiFi IP: ${intToIp(wifiInfo?.ipAddress ?: 0)}")

            // List all network interfaces
            Log.d(TAG, "=== Network Interfaces ===")
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { netInterface ->
                Log.d(TAG, "Interface: ${netInterface.name}")
                netInterface.inetAddresses?.toList()?.forEach { addr ->
                    Log.d(TAG, "  Address: ${addr.hostAddress}")
                }
            }

            // Test direct ping to printer
            val printerIp = "192.168.101.87"
            Log.d(TAG, "=== Testing direct connection to $printerIp ===")
            val reachable = InetAddress.getByName(printerIp).isReachable(3000)
            Log.d(TAG, "Printer reachable via ping: $reachable")

            // Test UDP broadcast
            Log.d(TAG, "=== Testing UDP broadcast ===")
            val socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 3000

            val message = "MP4200FIND".toByteArray()
            val broadcastAddress = InetAddress.getByName("255.255.255.255")
            val packet = DatagramPacket(message, message.size, broadcastAddress, 1460)

            Log.d(TAG, "Sending broadcast to 255.255.255.255:1460")
            socket.send(packet)

            // Listen for responses
            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)

            var responseCount = 0
            repeat(5) {
                try {
                    socket.receive(receivePacket)
                    responseCount++
                    val responseIp = receivePacket.address.hostAddress
                    val responseData = String(receivePacket.data, 0, receivePacket.length)
                    Log.d(TAG, "Received response #$responseCount from $responseIp: $responseData")
                } catch (e: Exception) {
                    Log.d(TAG, "No more responses (timeout or error)")
                    return@repeat
                }
            }

            socket.close()
            Log.d(TAG, "Total responses received: $responseCount")

            // Test direct connection to printer port
            Log.d(TAG, "=== Testing TCP connection to $printerIp:9100 ===")
            try {
                val testSocket = java.net.Socket()
                testSocket.connect(java.net.InetSocketAddress(printerIp, 9100), 3000)
                Log.d(TAG, "TCP connection to $printerIp:9100 successful!")
                testSocket.close()
            } catch (e: Exception) {
                Log.e(TAG, "TCP connection to $printerIp:9100 failed: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Debug scan failed", e)
        }
    }

    private fun intToIp(ip: Int): String {
        return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    }
}
