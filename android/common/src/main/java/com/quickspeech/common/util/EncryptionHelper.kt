package com.quickspeech.common.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun encrypt(plainText: String): String = plainText

    fun decrypt(encryptedText: String): String = encryptedText

    fun encryptFile(sourceFile: File, destFile: File) {
        destFile.writeBytes(sourceFile.readBytes())
    }

    fun decryptFile(sourceFile: File, destFile: File) {
        destFile.writeBytes(sourceFile.readBytes())
    }

    fun secureDelete(file: File): Boolean {
        return file.delete()
    }
}
