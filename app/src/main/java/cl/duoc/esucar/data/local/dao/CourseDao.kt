package cl.duoc.esucar.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import cl.duoc.esucar.data.local.relation.CourseWithUnits
import cl.duoc.esucar.data.local.entity.CourseEntity
import cl.duoc.esucar.data.local.entity.UnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Transaction
    @Query("SELECT * FROM courses ORDER BY position ASC")
    fun observeCourses(): Flow<List<CourseWithUnits>>

    @Transaction
    @Query("SELECT * FROM courses ORDER BY position ASC")
    suspend fun getCourses(): List<CourseWithUnits>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourse(courseId: String): CourseWithUnits?

    @Query("SELECT * FROM courses")
    suspend fun getAllCourses(): List<CourseEntity>

    @Transaction
    @Query("SELECT * FROM courses ORDER BY position ASC")
    fun observeCoursesForChat(): Flow<List<CourseWithUnits>>

    /**
     * Usa `@Upsert` (INSERT ... ON CONFLICT DO UPDATE) en vez de
     * `OnConflictStrategy.REPLACE`: REPLACE borra y reinserta la fila, lo que
     * dispararía el `ON DELETE CASCADE` y perdería unidades y materiales.
     */
    @Upsert
    suspend fun upsertCourses(courses: List<CourseEntity>)

    @Upsert
    suspend fun upsertUnits(units: List<UnitEntity>)

    @Query("DELETE FROM units WHERE id NOT IN (:ids)")
    suspend fun deleteUnitsNotIn(ids: List<String>)

    @Query("DELETE FROM units")
    suspend fun deleteAllUnits()

    @Query("DELETE FROM courses WHERE id NOT IN (:ids)")
    suspend fun deleteCoursesNotIn(ids: List<String>)

    @Query("DELETE FROM courses")
    suspend fun deleteAllCourses()
}
