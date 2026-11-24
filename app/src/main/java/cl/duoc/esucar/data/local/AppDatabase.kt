package cl.duoc.esucar.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.esucar.data.local.dao.CourseDao
import cl.duoc.esucar.data.local.dao.MaterialDao
import cl.duoc.esucar.data.local.dao.MessageDao
import cl.duoc.esucar.data.local.dao.NotificationDao
import cl.duoc.esucar.data.local.entity.CourseEntity
import cl.duoc.esucar.data.local.entity.MaterialEntity
import cl.duoc.esucar.data.local.entity.MessageEntity
import cl.duoc.esucar.data.local.entity.NotificationEntity
import cl.duoc.esucar.data.local.entity.UnitEntity
import cl.duoc.esucar.data.local.seed.SeedData
import androidx.room.migration.Migration
import cl.duoc.esucar.data.local.dao.UserDao
import cl.duoc.esucar.data.local.entity.UserEntity
import cl.duoc.esucar.data.local.dao.DirectMessageDao
import cl.duoc.esucar.data.local.entity.DirectMessageEntity

@Database(
    entities = [
        CourseEntity::class,
        UnitEntity::class,
        MaterialEntity::class,
        MessageEntity::class,
        NotificationEntity::class,
        UserEntity::class,
        DirectMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun courseDao(): CourseDao

    abstract fun materialDao(): MaterialDao

    abstract fun messageDao(): MessageDao

    abstract fun notificationDao(): NotificationDao

    abstract fun userDao(): UserDao

    abstract fun directMessageDao(): DirectMessageDao

    companion object {
        private const val DATABASE_NAME = "esucar.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL,
                passwordHash TEXT NOT NULL,
                passwordSalt TEXT NOT NULL,
                role TEXT NOT NULL,
                createdAt TEXT NOT NULL
            )
            """.trimIndent()
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Metadatos del archivo remoto (Supabase Storage) y copia local offline.
                db.execSQL("ALTER TABLE materials ADD COLUMN storagePath TEXT")
                db.execSQL("ALTER TABLE materials ADD COLUMN fileName TEXT")
                db.execSQL("ALTER TABLE materials ADD COLUMN mimeType TEXT")
                db.execSQL("ALTER TABLE materials ADD COLUMN localPath TEXT")
                // Autor de la consulta: la mensajería es por usuario, no por rol.
                db.execSQL("ALTER TABLE messages ADD COLUMN studentId TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE materials ADD COLUMN pendingProgressUserId TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE courses ADD COLUMN teacherId TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS direct_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        studentId TEXT NOT NULL,
                        teacherId TEXT NOT NULL,
                        senderId TEXT NOT NULL,
                        studentName TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp TEXT NOT NULL
                    )""".trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_direct_messages_studentId_teacherId_timestamp ON direct_messages(studentId, teacherId, timestamp)")
            }
        }

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        SeedData.insert(db)
                    }
                })
                .build()
    }
}
