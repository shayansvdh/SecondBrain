package com.secondbrain.planner

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    val day: Long? = null,
    val amount: Double = 0.0,
    val category: String = "",
    val priority: Int = 1,
    val progress: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "day"])
data class HabitLog(val habitId: Long, val day: Long)

@Dao
interface AppDao {
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun all(): Flow<List<Item>>

    @Query("SELECT * FROM habit_logs")
    fun logs(): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(i: Item): Long

    @Delete
    suspend fun delete(i: Item)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addLog(l: HabitLog)

    @Delete
    suspend fun delLog(l: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :id")
    suspend fun clearLogs(id: Long)

    @Query("DELETE FROM items")
    suspend fun clearItems()

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()
}

@Database(entities = [Item::class, HabitLog::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "brain.db")
                .build().also { inst = it }
        }
    }
}

enum class Kind(
    val fa: String,
    val single: String,
    val icon: ImageVector,
    val hasDay: Boolean = false,
    val dayLabel: String = "تاریخ",
    val defaultToday: Boolean = false,
    val amountLabel: String? = null,
    val hasProgress: Boolean = false,
    val hasCheck: Boolean = false,
    val hasPriority: Boolean = false,
    val categories: List<String> = emptyList()
) {
    TASK("کارها", "کار", Icons.Rounded.TaskAlt, hasDay = true, dayLabel = "موعد", defaultToday = true,
        hasCheck = true, hasPriority = true, categories = listOf("شخصی", "کسب و کار", "ورزش", "مهارت", "سفر")),
    PROJECT("پروژه‌ها", "پروژه", Icons.Rounded.Folder, hasDay = true, dayLabel = "مهلت",
        hasProgress = true, hasCheck = true, hasPriority = true, categories = listOf("شخصی", "کسب و کار", "مهارت", "سفر")),
    NOTE("نوت‌ها", "نوت", Icons.Rounded.StickyNote2, categories = listOf("ایده", "یادداشت", "مهم")),
    GOAL("اهداف", "هدف", Icons.Rounded.Flag, hasDay = true, dayLabel = "تاریخ هدف",
        hasProgress = true, hasCheck = true, hasPriority = true, categories = listOf("ورزش", "مالی", "رشد فردی", "کسب و کار")),
    HABIT("عادت‌ها", "عادت", Icons.Rounded.Repeat, categories = listOf("سلامت", "ذهن", "کار", "یادگیری")),
    JOURNAL("ژورنال", "ژورنال", Icons.Rounded.AutoStories, hasDay = true, defaultToday = true,
        categories = listOf("شکرگزاری", "مرور روز", "ایده")),
    INCOME("درآمدها", "درآمد", Icons.Rounded.TrendingUp, hasDay = true, defaultToday = true,
        amountLabel = "مبلغ (تومان)", categories = listOf("حقوق", "پروژه", "سرمایه‌گذاری", "سایر")),
    EXPENSE("هزینه‌ها", "هزینه", Icons.Rounded.TrendingDown, hasDay = true, defaultToday = true,
        amountLabel = "مبلغ (تومان)", categories = listOf("خوراک", "حمل‌ونقل", "مسکن", "سرگرمی", "آموزش", "سلامت", "سایر")),
    MEAL("غذا", "غذا", Icons.Rounded.Restaurant, hasDay = true, defaultToday = true,
        amountLabel = "کالری", categories = listOf("صبحانه", "ناهار", "شام", "میان‌وعده")),
    WEIGHT("وزن", "وزن", Icons.Rounded.MonitorWeight, hasDay = true, defaultToday = true,
        amountLabel = "وزن (کیلوگرم)"),
    BOOK("کتاب‌ها", "کتاب", Icons.Rounded.MenuBook, hasProgress = true, hasCheck = true,
        categories = listOf("رشد فردی", "کسب و کار", "رمان", "آموزشی")),
    SOURCE("منابع", "منبع", Icons.Rounded.Bookmark, categories = listOf("مقاله", "ویدیو", "پادکست", "ابزار"))
}
