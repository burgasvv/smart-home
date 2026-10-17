package org.burgas.client

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.milliseconds

object XiaomiMiIoClient {

    private const val PORT = 54321
    private const val TIMEOUT_MS = 5000L
    private const val HEADER = 32

    private var deviceId = 0
    private var stamp = 0

    private fun String.hexToBytes() = HexFormat.of().parseHex(this)

    private fun ByteArray.getInt32BE(off: Int) =
        ((this[off].toInt() and 0xFF) shl 24) or
                ((this[off + 1].toInt() and 0xFF) shl 16) or
                ((this[off + 2].toInt() and 0xFF) shl 8) or
                (this[off + 3].toInt() and 0xFF)

    private fun ByteArray.putInt32BE(off: Int, v: Int) {
        this[off] = (v ushr 24).toByte()
        this[off + 1] = (v ushr 16).toByte()
        this[off + 2] = (v ushr 8).toByte()
        this[off + 3] = v.toByte()
    }

    private fun deriveKeyAndIv(tokenHex: String): Pair<ByteArray, ByteArray> {
        val token = tokenHex.hexToBytes()
        val md = MessageDigest.getInstance("MD5")
        val key = md.digest(token)
        return key to md.digest(key + token)
    }

    private fun cipher(mode: Int, tokenHex: String): Cipher {
        val (key, iv) = deriveKeyAndIv(tokenHex)
        return Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
            init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        }
    }

    private fun encrypt(data: String, tokenHex: String): ByteArray =
        cipher(Cipher.ENCRYPT_MODE, tokenHex).doFinal(data.toByteArray())

    private fun decrypt(data: ByteArray, tokenHex: String): String =
        runCatching { String(cipher(Cipher.DECRYPT_MODE, tokenHex).doFinal(data)) }
            .getOrElse { "Ошибка дешифрования: ${it.message}" }

    private fun helloPacket(): ByteArray = ByteArray(HEADER) { 0xFF.toByte() }.apply {
        this[0] = 0x21
        this[1] = 0x31
        this[2] = 0x00
        this[3] = 0x20
    }

    private fun buildPacket(payload: ByteArray, tokenHex: String): ByteArray {
        val length = HEADER + payload.size
        val packet = ByteArray(length).apply {
            this[0] = 0x21
            this[1] = 0x31
            this[2] = (length ushr 8).toByte()
            this[3] = length.toByte()
            putInt32BE(8, deviceId)
            putInt32BE(12, stamp)
        }
        payload.copyInto(packet, HEADER)
        tokenHex.hexToBytes().copyInto(packet, 16)
        MessageDigest.getInstance("MD5").digest(packet).copyInto(packet, 16)
        return packet
    }

    private suspend fun ConnectedDatagramSocket.sendBytes(bytes: ByteArray, to: InetSocketAddress) =
        send(Datagram(Buffer().apply { write(bytes) }, to))

    private suspend fun ConnectedDatagramSocket.receiveBytes(): ByteArray? =
        withTimeoutOrNull(TIMEOUT_MS.milliseconds) { receive().packet.readByteArray() }

    private suspend fun sendHandshake(socket: ConnectedDatagramSocket, addr: InetSocketAddress): Boolean {
        socket.sendBytes(helloPacket(), addr)
        println("Handshake отправлен. Ожидание ответа...")

        val bytes = socket.receiveBytes()
        if (bytes == null) {
            println("Устройство не ответило на handshake (таймаут)."); return false
        }
        if (bytes.size < HEADER) {
            println("Ответ слишком короткий: ${bytes.size} байт."); return false
        }

        deviceId = bytes.getInt32BE(8)
        stamp = bytes.getInt32BE(12)
        println("Device ID: $deviceId, Stamp: $stamp")
        return true
    }

    suspend fun sendCommand(ip: String, tokenHex: String, command: JsonObjectBuilder.() -> Unit) = withContext(Dispatchers.IO) {
        val selector = SelectorManager(Dispatchers.IO)
        val addr = InetSocketAddress(ip, PORT)
        val socket = aSocket(selector).udp().connect(addr)

        try {
            if (!sendHandshake(socket, addr)) return@withContext

            val jsonCommand = buildJsonObject(command).toString()
            socket.sendBytes(buildPacket(encrypt(jsonCommand, tokenHex), tokenHex), addr)
            println("Команда отправлена. Ожидание ответа...")

            val bytes = socket.receiveBytes()
            if (bytes == null) {
                println("Устройство не ответило на команду (таймаут)."); return@withContext
            }

            println("Получен ответ (${bytes.size} байт)")
            if (bytes.size > HEADER) {
                val decrypted = decrypt(bytes.copyOfRange(HEADER, bytes.size), tokenHex)
                println("Расшифрованный ответ: $decrypted")
            }
        } catch (e: Exception) {
            println("Ошибка: ${e.message}")
            e.printStackTrace()
        } finally {
            socket.close()
            selector.close()
        }
    }
}