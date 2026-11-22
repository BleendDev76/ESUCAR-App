package cl.duoc.esucar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import cl.duoc.esucar.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE role = :role ORDER BY timestamp DESC")
    fun observeByRole(role: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE role = :role AND read = 0")
    fun observeUnreadCount(role: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE role = :role")
    suspend fun getAllByRole(role: String): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notification: NotificationEntity)

    @Upsert
    suspend fun upsertAll(notifications: List<NotificationEntity>)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("UPDATE notifications SET read = 1 WHERE id = :notificationId")
    suspend fun markRead(notificationId: String)

    @Query("UPDATE notifications SET read = 1 WHERE role = :role")
    suspend fun markAllRead(role: String)
}
