package com.flightmaximizer.data.local.db.converter

import androidx.room.TypeConverter
import java.time.LocalDate

class DateConverter {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()
}
