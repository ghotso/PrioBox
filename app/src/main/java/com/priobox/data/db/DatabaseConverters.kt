package com.priobox.data.db

import androidx.room.TypeConverter
import com.priobox.data.model.MailSecurity
import java.util.Date

class DatabaseConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromSecurity(value: MailSecurity?): String? = value?.name

    @TypeConverter
    fun toSecurity(value: String?): MailSecurity? =
        value?.let {
            runCatching { MailSecurity.valueOf(it) }.getOrDefault(MailSecurity.NONE)
        }
}

