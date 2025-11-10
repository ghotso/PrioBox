package com.vipmail.data.db

import androidx.room.TypeConverter
import java.util.Date

class DatabaseConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}

