package com.quickspeech.common.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "quickspeech_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String): String {
        val combined = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)
        val iv = ByteArray(12)
        val encrypted = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, encrypted, 0, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = encryptedPrefs.getString("db_encryption_key", null)
        if (existingKey != null) {
            val keyBytes = android.util.Base64.decode(existingKey, android.util.Base64.NO_WRAP)
            return javax.crypto.SecretKeySpec(keyBytes, "AES")
        }
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val secretKey = keyGenerator.generateKey()
        val encoded = secretKey.encoded
        encryptedPrefs.edit().putString("db_encryption_key", android.util.Base64.encodeToString(encoded, android.util.Base64.NO_WRAP)).apply()
        return secretKey
    }

    fun encryptFile(sourceFile: File, destFile: File) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val inputBytes = sourceFile.readBytes()
        val encrypted = cipher.doFinal(inputBytes)
        val output = ByteArray(cipher.iv.size + encrypted.size)
        System.arraycopy(cipher.iv, 0, output, 0, cipher.iv.size)
        System.arraycopy(encrypted, 0, output, cipher.iv.size, encrypted.size)
        destFile.writeBytes(output)
    }

    fun decryptFile(sourceFile: File, destFile: File) {
        val combined = sourceFile.readBytes()
        val iv = ByteArray(12)
        val encrypted = ByteArray(combined.size - 12)
        System.arraycopy(combined, 0, iv, 0, 12)
        System.arraycopy(combined, 12, encrypted, 0, encrypted.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        destFile.writeBytes(cipher.doFinal(encrypted))
    }

    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true
        val length = file.length()
        val random = SecureRandom()
        try {
            val raf = java.io.RandomAccessFile(file, "rw")
            for (pass in 0 until 3) {
                raf.seek(0)
                val buffer = ByteArray(4096)
                var written = 0L
                while (written < length) {
                    random.nextBytes(buffer)
                    val toWrite = minOf(buffer.size.toLong(), length - written).toInt()
                    raf.write(buffer, 0, toWrite)
                    written += toWrite
                }
                raf.fd.sync()
            }
            raf.close()
        } catch (_: Exception) { }
        return file.delete()
    }
}
