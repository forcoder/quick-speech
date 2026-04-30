package com.quickspeech.input.ai.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_reply_preferences")

@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val REPLY_MODE_KEY = intPreferencesKey("reply_mode")
        private val AI_PANEL_ENABLED_KEY = booleanPreferencesKey("ai_panel_enabled")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }

    val replyModeFlow: Flow<ReplyMode> = context.dataStore.data.map { prefs ->
        ReplyMode.fromOrdinal(prefs[REPLY_MODE_KEY] ?: ReplyMode.HYBRID.ordinal)
    }

    val isAiPanelEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AI_PANEL_ENABLED_KEY] ?: true
    }

    val userIdFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_ID_KEY] ?: ""
    }

    suspend fun setReplyMode(mode: ReplyMode) {
        context.dataStore.edit { prefs ->
            prefs[REPLY_MODE_KEY] = mode.ordinal
        }
    }

    suspend fun setAiPanelEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AI_PANEL_ENABLED_KEY] = enabled
        }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID_KEY] = userId
        }
    }
}
