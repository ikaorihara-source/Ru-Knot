package com.ikaorihara.ruknot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.notification.NotificationRecord
import com.ikaorihara.ruknot.streamer.StreamerRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 类名: StreamerRoomDatabase (你的自定义名字)
// 父类: RoomDatabase (安卓系统的名字)
// ↓↓↓ 这样写是完美的，不会冲突 ↓↓↓
@Database(
    entities = [StreamerRoom::class, AlarmRule::class, NotificationRecord::class],
    version = 5,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 1, to = 2) //, spec = AppDatabase.MyRenameMigration::class
//    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun StreamerDAO(): StreamerDAO
    abstract fun AlarmDAO(): AlarmDAO
    abstract fun NotificationDao(): NotificationDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 定义从版本 2 升级到 3 的手动迁移逻辑
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE streamer_room_table ADD COLUMN is_vibration_only INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE streamer_room_table SET is_vibration_only = 0")
            }
        }

        // ★ 3. 添加版本 3 升 4 的迁移脚本
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `message` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `room_id` INTEGER NOT NULL, 
                        `user_id` INTEGER NOT NULL, 
                        `dynamic_id` TEXT, 
                        `avatar_url` TEXT NOT NULL
                    )
                """.trimIndent()
                )
            }
        }

        // ★ 版本 4 升 5 的迁移脚本 (给表加上 is_locked 字段)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notification_history` ADD COLUMN `is_locked` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blive_alarm_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(AppDatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // 定义回调类
        private class AppDatabaseCallback() : Callback() {

            // 这个方法只会在数据库第一次创建时调用 (全新安装)
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    // 启动协程在后台写入默认数据
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database.StreamerDAO(), database.AlarmDAO())
                    }
                }
            }
        }

        // 填充默认数据的方法
        suspend fun populateDatabase(streamerDao: StreamerDAO, alarmDao: AlarmDAO) {
            // 插入默认主播：折原露露 (Orihara Ruru)
            val defaultRoomId = 22389206L

            val defaultStreamer = StreamerRoom(
                roomId = defaultRoomId,
                userId = 631070414L, // 折原露露的 UID
                userName = "折原露露", // 初始名字，联网后会自动更新
                title = "我将永远热爱音乐",
                coverUrl = "", // 暂时留空，联网自动拉取
                avatarUrl = "",
                isLive = false,
                ringtoneUri = null, // 使用默认铃声
                isVibrationOnly = false,
                follower = "-",
                likeNum = "-",
                isPinned = true,
                isLocked = true,
                customOrder = -2147483648
            )
            streamerDao.insertStreamer(defaultStreamer)

            // 插入默认闹钟规则
            val defaultAlarm = AlarmRule(
                roomId = defaultRoomId,
                isEnabled = false,
                alarmName = "我爱露露！",
                startTime = "00:00",
                endTime = "00:00", // 早上7点到9点
                endDayOffset = 1,
                type = AlarmType.REPEAT, // 每天重复
                repeatPayload = "1,2,3,4,5,6,7", // 周一到周日
                keywords = "",
                volume = 0 // 默认使用全局音量
            )
            alarmDao.insertRule(defaultAlarm)
        }
    }

//    @RenameColumn(tableName = "alarm_rules_table", fromColumnName = "volume", toColumnName = "audio_level")
//    class MyRenameMigration : AutoMigrationSpec
}