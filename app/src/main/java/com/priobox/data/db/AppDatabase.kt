package com.priobox.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.priobox.data.db.dao.EmailAccountDao
import com.priobox.data.db.dao.EmailFolderDao
import com.priobox.data.db.dao.EmailMessageDao
import com.priobox.data.db.dao.VipSenderDao
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailFolder
import com.priobox.data.model.EmailMessage
import com.priobox.data.model.VipSender

@Database(
    entities = [
        EmailAccount::class,
        EmailMessage::class,
        VipSender::class,
        EmailFolder::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emailAccountDao(): EmailAccountDao
    abstract fun emailMessageDao(): EmailMessageDao
    abstract fun vipSenderDao(): VipSenderDao
    abstract fun emailFolderDao(): EmailFolderDao
}

