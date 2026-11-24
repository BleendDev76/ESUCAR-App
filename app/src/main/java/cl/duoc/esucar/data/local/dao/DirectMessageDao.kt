package cl.duoc.esucar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import cl.duoc.esucar.data.local.entity.DirectMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectMessageDao {
    @Query("SELECT * FROM direct_messages ORDER BY timestamp ASC")
    fun observeAll(): Flow<List<DirectMessageEntity>>

    @Upsert
    suspend fun upsertAll(messages: List<DirectMessageEntity>)

    @Query("DELETE FROM direct_messages")
    suspend fun clearAll()
}
