package com.quickspeech.wubi.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 五笔词库表
 * 存储编码、汉字/词组、词频、类型
 */
@Entity(
    tableName = "wubi_words",
    indices = [
        Index(value = ["code"], name = "idx_code"),
        Index(value = ["word"], name = "idx_word"),
        Index(value = ["code", "word"], name = "idx_code_word", unique = true)
    ]
)
data class WubiWordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 五笔编码（小写字母，如 "aaaa"） */
    @ColumnInfo(name = "code")
    val code: String,

    /** 对应的汉字或词组 */
    @ColumnInfo(name = "word")
    val word: String,

    /** 词频（越高越靠前） */
    @ColumnInfo(name = "frequency")
    val frequency: Int = 0,

    /** 类型：0=单字, 1=二字词, 2=三字词, 3=多字词 */
    @ColumnInfo(name = "type")
    val type: Int = 0,

    /** 是否为简码（一级/二级简码标记） */
    @ColumnInfo(name = "simple_code")
    val simpleCode: Boolean = false
)

/**
 * 用户词频学习表
 * 记录用户对词语的选择偏好
 */
@Entity(
    tableName = "user_frequency",
    indices = [
        Index(value = ["word"], name = "idx_uf_word", unique = true)
    ]
)
data class UserFrequencyEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 用户选择的词 */
    @ColumnInfo(name = "word")
    val word: String,

    /** 用户选择次数 */
    @ColumnInfo(name = "count")
    val count: Int = 1,

    /** 编码（用于快速查找） */
    @ColumnInfo(name = "code")
    val code: String = "",

    /** 最后使用时间戳 */
    @ColumnInfo(name = "last_used")
    val lastUsed: Long = System.currentTimeMillis()
)

/**
 * 最近使用词表
 * 用于"最近使用优先"排序
 */
@Entity(
    tableName = "recent_words",
    indices = [
        Index(value = ["word"], name = "idx_rw_word", unique = true)
    ]
)
data class RecentWordEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "code")
    val code: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
