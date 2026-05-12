package com.quickspeech.input.ai

import android.util.Log
import com.quickspeech.common.db.BehaviorRecordDao
import com.quickspeech.common.db.BehaviorRecordEntity
import com.quickspeech.common.db.UserActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BehaviorRecorder @Inject constructor(
    private val behaviorRecordDao: BehaviorRecordDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun recordAccepted(
        originalReply: String,
        sceneType: String = "general",
        contextPrompt: String? = null
    ) {
        scope.launch {
            try {
                behaviorRecordDao.insert(
                    BehaviorRecordEntity(
                        originalReply = originalReply,
                        userAction = UserActionType.ACCEPTED,
                        sceneType = sceneType,
                        contextPrompt = contextPrompt
                    )
                )
            } catch (e: Exception) {
                Log.d("BehaviorRecorder", "recordAccepted failed", e)
            }
        }
    }

    fun recordSkipped(
        originalReply: String,
        sceneType: String = "general",
        contextPrompt: String? = null
    ) {
        scope.launch {
            try {
                behaviorRecordDao.insert(
                    BehaviorRecordEntity(
                        originalReply = originalReply,
                        userAction = UserActionType.SKIPPED,
                        sceneType = sceneType,
                        contextPrompt = contextPrompt
                    )
                )
            } catch (e: Exception) {
                Log.d("BehaviorRecorder", "recordSkipped failed", e)
            }
        }
    }

    fun recordModified(
        originalReply: String,
        modifiedReply: String,
        sceneType: String = "general",
        contextPrompt: String? = null
    ) {
        scope.launch {
            try {
                behaviorRecordDao.insert(
                    BehaviorRecordEntity(
                        originalReply = originalReply,
                        userAction = UserActionType.MODIFIED,
                        modifiedReply = modifiedReply,
                        sceneType = sceneType,
                        contextPrompt = contextPrompt
                    )
                )
            } catch (e: Exception) {
                Log.d("BehaviorRecorder", "recordModified failed", e)
            }
        }
    }

    fun recordSelfWritten(
        originalReply: String,
        selfWrittenReply: String,
        sceneType: String = "general",
        contextPrompt: String? = null
    ) {
        scope.launch {
            try {
                behaviorRecordDao.insert(
                    BehaviorRecordEntity(
                        originalReply = originalReply,
                        userAction = UserActionType.SELF_WRITTEN,
                        selfWrittenReply = selfWrittenReply,
                        sceneType = sceneType,
                        contextPrompt = contextPrompt
                    )
                )
            } catch (e: Exception) {
                Log.d("BehaviorRecorder", "recordSelfWritten failed", e)
            }
        }
    }
}
