package com.quickspeech.wubi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户自定义规则表
 * 存储快捷输入规则：输入 shortcut 自动展开为 expansion
 */
@Entity(
    tableName = "user_rules",
    indices = [
        Index(value = ["shortcut"], name = "idx_rule_shortcut", unique = true),
        Index(value = ["category"], name = "idx_rule_category")
    ]
)
data class UserRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 快捷触发词，如 "addr", "sig1", "date" */
    @ColumnInfo(name = "shortcut")
    val shortcut: String,

    /** 展开后的完整文本 */
    @ColumnInfo(name = "expansion")
    val expansion: String,

    /** 分类：general, address, signature, template */
    @ColumnInfo(name = "category")
    val category: String = "general",

    /** 规则描述 */
    @ColumnInfo(name = "description")
    val description: String = "",

    /** 使用次数 */
    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    /** 创建时间戳 */
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    /** 更新时间戳 */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    /** 是否启用 */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)
