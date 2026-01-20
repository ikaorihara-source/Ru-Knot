package com.ikaorihara.ruknot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ikaorihara.ruknot.alarm.AlarmRule
import com.ikaorihara.ruknot.streamer.StreamerRoom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 类名: StreamerRoomDatabase (你的自定义名字)
// 父类: RoomDatabase (安卓系统的名字)
// ↓↓↓ 这样写是完美的，不会冲突 ↓↓↓
@Database(
    entities = [StreamerRoom::class, AlarmRule::class],
    version = 3,
    exportSchema = true,
//    autoMigrations = [
//        AutoMigration(from = 1, to = 2) //, spec = AppDatabase.MyRenameMigration::class
//    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun StreamerDAO(): StreamerDAO
    abstract fun AlarmDAO(): AlarmDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 定义从版本 2 升级到 3 的手动迁移逻辑
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
//                // 1. 加 is_locked 列
//                db.execSQL("ALTER TABLE streamer_room_table ADD COLUMN is_locked INTEGER NOT NULL DEFAULT 0")
//
//                // 2. 加 custom_order 列
//                // 默认值设为 0
//                db.execSQL("ALTER TABLE streamer_room_table ADD COLUMN custom_order INTEGER NOT NULL DEFAULT 0")
//
//                // 3. 初始化数据
//                // 3.1 把露露锁上
//                db.execSQL("UPDATE streamer_room_table SET is_locked = 1 WHERE room_id = 22389206")
//
//                // 3.2 初始化排序
//                // 我们直接把 room_id 赋值给 custom_order，作为初始顺序
//                // 这样升级上来时，顺序就是添加的顺序
//                db.execSQL("UPDATE streamer_room_table SET custom_order = room_id")
//
//                // 3.3 强行把露露排第一
//                // 给她一个负数，确保她永远比 room_id (正数) 小
//                db.execSQL("UPDATE streamer_room_table SET custom_order = -2147483648 WHERE room_id = 22389206")

                db.execSQL("ALTER TABLE streamer_room_table ADD COLUMN is_vibration_only INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE streamer_room_table SET is_vibration_only = 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "blive_alarm_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(AppDatabaseCallback())
//                .fallbackToDestructiveMigration()
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