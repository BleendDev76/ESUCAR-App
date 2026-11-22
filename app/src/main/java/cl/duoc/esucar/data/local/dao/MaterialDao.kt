package cl.duoc.esucar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import cl.duoc.esucar.data.local.entity.MaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaterialDao {

    @Query("SELECT * FROM materials WHERE unitId = :unitId ORDER BY position ASC")
    fun observeByUnit(unitId: String): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE downloaded = 1 ORDER BY title ASC")
    fun observeDownloaded(): Flow<List<MaterialEntity>>

    @Query("SELECT * FROM materials WHERE id = :materialId LIMIT 1")
    suspend fun getById(materialId: String): MaterialEntity?

    @Query("SELECT MIN(position) FROM materials WHERE unitId = :unitId")
    suspend fun minPosition(unitId: String): Int?

    @Query("SELECT MAX(position) FROM materials WHERE unitId = :unitId")
    suspend fun maxPosition(unitId: String): Int?

    @Query("SELECT * FROM materials")
    suspend fun getAll(): List<MaterialEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(material: MaterialEntity)

    @Update
    suspend fun update(material: MaterialEntity)

    @Upsert
    suspend fun upsertAll(materials: List<MaterialEntity>)

    @Query("DELETE FROM materials WHERE id = :materialId")
    suspend fun deleteById(materialId: String)

    @Query("DELETE FROM materials WHERE id NOT IN (:ids)")
    suspend fun deleteNotIn(ids: List<String>)

    @Query("UPDATE materials SET studied = 1 WHERE id = :materialId AND studied = 0")
    suspend fun markStudied(materialId: String)

    @Query("UPDATE materials SET studied = 0")
    suspend fun resetStudied()

    @Query("UPDATE materials SET downloaded = :downloaded WHERE id = :materialId")
    suspend fun setDownloaded(materialId: String, downloaded: Boolean)

    @Query("UPDATE materials SET localPath = :localPath, downloaded = :downloaded WHERE id = :materialId")
    suspend fun setLocalFile(materialId: String, localPath: String?, downloaded: Boolean)

    @Query("UPDATE materials SET downloaded = NOT downloaded WHERE id = :materialId")
    suspend fun toggleDownload(materialId: String)
}
