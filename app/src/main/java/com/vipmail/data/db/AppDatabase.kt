package com.vipmail.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vipmail.data.db.dao.EmailAccountDao
import com.vipmail.data.db.dao.EmailMessageDao
import com.vipmail.data.db.dao.VipSenderDao
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.EmailMessage
import com.vipmail.data.model.VipSender

@Database(
    entities = [
        EmailAccount::class,
        EmailMessage::class,
        VipSender::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailAccountDao(): EmailAccountDao
    abstract fun emailMessageDao(): EmailMessageDao
    abstract fun vipSenderDao(): VipSenderDao
}

