package com.billarlegends.portfolio.data.local

import androidx.room.TypeConverter
import com.billarlegends.portfolio.domain.MovementType
import com.billarlegends.portfolio.domain.Source

class Converters {
    @TypeConverter
    fun fromSource(value: Source): String = value.name

    @TypeConverter
    fun toSource(value: String): Source = Source.valueOf(value)

    @TypeConverter
    fun fromMovementType(value: MovementType): String = value.name

    @TypeConverter
    fun toMovementType(value: String): MovementType = MovementType.valueOf(value)
}
