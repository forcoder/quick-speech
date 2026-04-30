package com.quickspeech.wubi.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 云词库同步 API 接口（预留）
 * 用于从云端下载最新词库、上传用户词频数据、获取热门词组更新
 */
interface CloudSyncApi {

    /**
     * 获取最新词库更新
     * @param version 当前词库版本号
     * @return 词库增量更新数据
     */
    @GET("/api/wubi/dict/updates")
    suspend fun getDictUpdates(
        @Query("version") version: Int,
        @Query("scheme") scheme: Int = 86 // 86版/98版
    ): DictUpdateResponse

    /**
     * 上传用户词频数据（匿名统计，用于优化词库）
     */
    @POST("/api/wubi/stats/upload")
    suspend fun uploadUserStats(
        @Body body: UserStatsUpload
    ): SyncResponse

    /**
     * 获取热门搜索词（用于优化排序）
     */
    @GET("/api/wubi/trending")
    suspend fun getTrendingWords(
        @Query("limit") limit: Int = 100
    ): TrendingWordsResponse

    /**
     * 同步用户个人词库（登录用户）
     */
    @POST("/api/wubi/user/sync")
    suspend fun syncUserDictionary(
        @Body body: UserDictSyncRequest
    ): UserDictSyncResponse
}

data class DictUpdateResponse(
    val version: Int,
    val entries: List<WubiWordEntry>,
    val deletedIds: List<Long>
)

data class SyncResponse(
    val success: Boolean,
    val message: String
)

data class UserStatsUpload(
    val deviceId: String,
    val selections: List<SelectionStat>
)

data class SelectionStat(
    val word: String,
    val code: String,
    val count: Int,
    val timestamp: Long
)

data class TrendingWordsResponse(
    val words: List<TrendingWord>
)

data class TrendingWord(
    val word: String,
    val code: String,
    val searchCount: Int
)

data class UserDictSyncRequest(
    val userId: String,
    val userFrequencies: List<UserFrequencyEntry>,
    val recentWords: List<RecentWordEntry>
)

data class UserDictSyncResponse(
    val serverFrequencies: List<UserFrequencyEntry>,
    val serverRecentWords: List<RecentWordEntry>,
    val lastSyncTimestamp: Long
)
