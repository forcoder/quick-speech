package com.quickspeech.common.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // TODO: Replace with real encryption (AES-256-GCM via Android Keystore)
    fun encrypt(plainText: String): String = plainText

    fun decrypt(encryptedText: String): String = encryptedText

    fun encryptFile(sourceFile: File, destFile: File) {
        destFile.writeBytes(sourceFile.readBytes())
    }

    fun decryptFile(sourceFile: File, destFile: File) {
        destFile.writeBytes(sourceFile.readBytes())
    }

    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true
        try {
            val length = file.length()
            if (length > 0) {
                val random = SecureRandom()
                val buffer = ByteArray(1024)
                java.io.RandomAccessFile(file, "rw").use { raf ->
                    var written = 0L
                    while (written < length) {
                        random.nextBytes(buffer)
                        val toWrite = minOf(buffer.size.toLong(), length - written).toInt()
                        raf.write(buffer, 0, toWrite)
                        written += toWrite
                    }
                }
            }
        } catch (_: Exception) { /* best-effort overwrite */ }
        return file.delete()
    }
}
