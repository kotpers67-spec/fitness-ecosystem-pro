package com.trainerapp.pro.data.sync

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Защищенное хранилище учетных данных и шифрование данных облачной синхронизации.
 * Данные на Google Диске передаются и хранятся в зашифрованном виде (AES-256).
 */
object CloudSecurityManager {

    private val MASK: ByteArray = byteArrayOf(
        0x53, 0x70, 0x69, 0x72, 0x69, 0x74, 0x46, 0x69, 0x74, 0x32, 0x30, 0x32, 0x36
    )

    private val ENC_ENDPOINT = byteArrayOf(
        0x3b, 0x04, 0x1d, 0x02, 0x1a, 0x4e, 0x69, 0x46, 0x07, 0x51, 0x42, 0x5b, 0x46, 
        0x27, 0x5e, 0x0e, 0x1d, 0x06, 0x13, 0x2a, 0x0c, 0x5a, 0x51, 0x5f, 0x5f, 0x19, 
        0x3e, 0x11, 0x0a, 0x00, 0x06, 0x07, 0x69, 0x1a, 0x5b, 0x73, 0x7b, 0x54, 0x4f, 
        0x30, 0x12, 0x11, 0x44, 0x25, 0x37, 0x24, 0x3f, 0x18, 0x4a, 0x6a, 0x53, 0x1b, 
        0x1e, 0x03, 0x3e, 0x00, 0x39, 0x44, 0x17, 0x05, 0x3a, 0x5d, 0x45, 0x78, 0x41, 
        0x30, 0x35, 0x0a, 0x28, 0x3f, 0x07, 0x24, 0x30, 0x3c, 0x5e, 0x7f, 0x06, 0x7e, 
        0x24, 0x38, 0x39, 0x36, 0x2d, 0x01, 0x12, 0x5f, 0x2b, 0x56, 0x40, 0x02, 0x70, 
        0x17, 0x25, 0x22, 0x26, 0x04, 0x21, 0x19, 0x28, 0x0c, 0x07, 0x46, 0x55, 0x01, 
        0x16, 0x20, 0x5f, 0x5d, 0x0c, 0x0c, 0x23, 0x0a
    )

    private val ENC_KEY = byteArrayOf(
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x73, 0x5d, 0x40, 0x0b, 0x70, 0x00, 0x06, 
        0x62, 0x41, 0x29, 0x40, 0x58, 0x47, 0x06
    )

    fun getEndpointUrl(): String {
        val decrypted = ByteArray(ENC_ENDPOINT.size)
        for (i in ENC_ENDPOINT.indices) {
            decrypted[i] = (ENC_ENDPOINT[i].toInt() xor MASK[i % MASK.size].toInt()).toByte()
        }
        return String(decrypted, StandardCharsets.UTF_8)
    }

    fun getSecretKey(): String {
        val decrypted = ByteArray(ENC_KEY.size)
        for (i in ENC_KEY.indices) {
            decrypted[i] = (ENC_KEY[i].toInt() xor MASK[i % MASK.size].toInt()).toByte()
        }
        return String(decrypted, StandardCharsets.UTF_8)
    }

    fun encryptPayload(plainText: String): String {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(getSecretKey().toByteArray(StandardCharsets.UTF_8))
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
            val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            "ENC:" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (_: Exception) {
            plainText
        }
    }

    fun decryptPayload(cipherText: String): String {
        val trimmed = cipherText.trim()
        if (!trimmed.startsWith("ENC:")) return trimmed
        val base64Data = trimmed.removePrefix("ENC:")
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(getSecretKey().toByteArray(StandardCharsets.UTF_8))
            val secretKeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
            val decoded = Base64.decode(base64Data, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            trimmed
        }
    }
}
