package com.quickspeech.input.context

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import com.quickspeech.input.ai.data.AppCategory
import com.quickspeech.input.ai.data.AppCategoryDao
import com.quickspeech.input.ai.data.AppCategoryMapping
import com.quickspeech.input.ai.data.AppCategoryMappingEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppContextDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCategoryDao: AppCategoryDao
) {
    private val usageStatsManager: UsageStatsManager? by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private val emailPackages = setOf(
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.samsung.android.email.provider",
        "com.huawei.android.email",
        "com.android.email",
        "com.netease.mobimail",
        "com.tencent.androidqqmail"
    )

    private val imPackages = setOf(
        "com.tencent.mm",
        "com.tencent.mobileqq",
        "com.sina.weibo",
        "com.eg.android.AlipayGphone",
        "com.snapchat.android",
        "com.whatsapp",
        "com.telegram.messenger",
        "com.discord",
        "com.slack",
        "com.twitter.android",
        "org.telegram.messenger",
        "com.ss.android.ugc.aweme"
    )

    private val documentPackages = setOf(
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint",
        "cn.wps.moffice_eng",
        "com.google.android.apps.docs",
        "com.adobe.reader",
        "com.kingsoft.moffice"
    )

    fun getCurrentForegroundApp(): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60

        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime) ?: return null
        var lastForegroundPackage: String? = null
        val event = UsageEvents.Event()

        while (usageEvents.getNextEvent(event)) {
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastForegroundPackage = event.packageName
            }
        }

        return lastForegroundPackage
    }

    suspend fun getAppCategory(packageName: String): AppCategory {
        val cached = appCategoryDao.getCategory(packageName)
        if (cached != null) {
            return AppCategory.valueOf(cached.category)
        }

        val category = classifyApp(packageName)
        appCategoryDao.insertMapping(
            AppCategoryMappingEntity(
                packageName = packageName,
                category = category.name
            )
        )
        return category
    }

    private fun classifyApp(packageName: String): AppCategory {
        return when {
            emailPackages.contains(packageName) -> AppCategory.EMAIL
            imPackages.contains(packageName) -> AppCategory.INSTANT_MESSAGING
            documentPackages.contains(packageName) -> AppCategory.DOCUMENT
            else -> classifyByAppName(packageName)
        }
    }

    private fun classifyByAppName(packageName: String): AppCategory {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val appName = pm.getApplicationLabel(appInfo).toString().lowercase()

            when {
                appName.contains("mail") || appName.contains("邮件") || appName.contains("邮箱") -> AppCategory.EMAIL
                appName.contains("word") || appName.contains("doc") || appName.contains("文档") ||
                        appName.contains("sheet") || appName.contains("excel") || appName.contains("表格") ||
                        appName.contains("slide") || appName.contains("ppt") || appName.contains("演示") -> AppCategory.DOCUMENT
                else -> AppCategory.OTHER
            }
        } catch (e: PackageManager.NameNotFoundException) {
            AppCategory.OTHER
        }
    }

    fun getCategoryStrategy(category: AppCategory): String {
        return when (category) {
            AppCategory.EMAIL -> "formal"
            AppCategory.INSTANT_MESSAGING -> "casual"
            AppCategory.DOCUMENT -> "professional"
            AppCategory.OTHER -> "neutral"
        }
    }
}
