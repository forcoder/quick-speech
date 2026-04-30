package com.quickspeech.wubi.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore by preferencesDataStore(name = "wubi_sync")

/**
 * 云词库同步管理器（预留实现）
 * 负责管理词库版本检查、增量更新、用户数据上传等
 */
@Singleton
class CloudSyncManager @Inject constructor(
    private val context: Context,
    private val dao: WubiDao
) {
    companion object {
        private val DICT_VERSION_KEY = intPreferencesKey("dict_version")
        private val LAST_SYNC_KEY = longPreferencesKey("last_sync_time")
    }

    /**
     * 检查词库是否需要更新
     */
    suspend fun checkForUpdates(): Boolean = withContext(Dispatchers.IO) {
        val currentVersion = context.syncDataStore.data.first()[DICT_VERSION_KEY] ?: 0
        // TODO: 调用 CloudSyncApi.getDictUpdates 检查是否有新版本
        false
    }

    /**
     * 执行词库增量更新
     */
    suspend fun performDictUpdate(): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: 从云端获取增量更新
            // val updates = api.getDictUpdates(currentVersion)
            // dao.insertWords(updates.entries)
            // 更新本地版本号
            // context.syncDataStore.edit { it[DICT_VERSION_KEY] = updates.version }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 上传用户统计数据
     */
    suspend fun uploadStats(): Boolean = withContext(Dispatchers.IO) {
        try {
            // TODO: 收集用户统计数据并上传
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取同步状态信息
     */
    suspend fun getSyncStatus(): SyncStatus = withContext(Dispatchers.IO) {
        val prefs = context.syncDataStore.data.first()
        SyncStatus(
            dictVersion = prefs[DICT_VERSION_KEY] ?: 0,
            lastSyncTime = prefs[LAST_SYNC_KEY] ?: 0L,
            wordCount = dao.getWordCount()
        )
    }

    /**
     * 重置同步状态
     */
    suspend fun resetSyncState() {
        context.syncDataStore.edit { prefs ->
            prefs.remove(DICT_VERSION_KEY)
            prefs.remove(LAST_SYNC_KEY)
        }
    }
}

data class SyncStatus(
    val dictVersion: Int,
    val lastSyncTime: Long,
    val wordCount: Int
)
