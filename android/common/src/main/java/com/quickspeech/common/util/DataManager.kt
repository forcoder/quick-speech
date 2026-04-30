package com.quickspeech.common.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.quickspeech.common.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val encryptionHelper: EncryptionHelper
) {

    suspend fun exportUserData(): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "quickspeech_data_$timestamp.json")

        val behaviorRecords = database.behaviorRecordDao().getAllRecords().first()
        val styleProfile = database.styleProfileDao().getProfileSync()
        val editHistory = database.editHistoryDao().getAllHistory().first()
        val corrections = database.correctionDao().getAllCorrections().first()

        val json = com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
            mapOf(
                "exportTimestamp" to System.currentTimeMillis(),
                "behaviorRecords" to behaviorRecords,
                "styleProfile" to styleProfile,
                "editHistory" to editHistory,
                "corrections" to corrections
            )
        )

        val encrypted = encryptionHelper.encrypt(json)
        exportFile.writeText(encrypted)
        exportFile
    }

    fun getShareIntent(exportFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    suspend fun deleteAllUserData(): Boolean = withContext(Dispatchers.IO) {
        try {
            database.behaviorRecordDao().deleteAll()
            database.styleProfileDao().deleteAll()
            database.editHistoryDao().deleteAll()
            database.correctionDao().deleteAll()

            val exportDir = File(context.cacheDir, "exports")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { encryptionHelper.secureDelete(it) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
