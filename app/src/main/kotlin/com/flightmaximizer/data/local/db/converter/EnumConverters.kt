package com.flightmaximizer.data.local.db.converter

import androidx.room.TypeConverter
import java.time.Instant

class EnumConverters {

    @TypeConverter
    fun fromInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toInstant(instant: Instant?): Long? = instant?.toEpochMilli()
}
