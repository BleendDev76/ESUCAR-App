package cl.duoc.esucar.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import cl.duoc.esucar.data.local.entity.CourseEntity
import cl.duoc.esucar.data.local.entity.MaterialEntity
import cl.duoc.esucar.data.local.entity.UnitEntity

data class UnitWithMaterials(
    @Embedded val unit: UnitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "unitId"
    )
    val materials: List<MaterialEntity>
)

data class CourseWithUnits(
    @Embedded val course: CourseEntity,
    @Relation(
        entity = UnitEntity::class,
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val units: List<UnitWithMaterials>
)
