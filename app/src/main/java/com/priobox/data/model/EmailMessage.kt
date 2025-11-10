package com.priobox.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "email_messages",
    indices = [
        Index("accountId"),
        Index("sender"),
        Index(value = ["accountId", "folder", "uid"], unique = true)
    ]
)
data class EmailMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val uid: String,
    val folder: String = EmailFolder.INBOX_SERVER_ID,
    val sender: String,
    val subject: String,
    val preview: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val isVip: Boolean = false
)

