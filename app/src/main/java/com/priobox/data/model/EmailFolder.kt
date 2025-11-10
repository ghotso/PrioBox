package com.priobox.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "email_folders",
    indices = [
        Index(value = ["accountId", "serverId"], unique = true)
    ]
)
data class EmailFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val serverId: String,
    val displayName: String,
    val selectable: Boolean = true,
    val typeFlags: Int = 0
) {
    companion object {
        const val INBOX_SERVER_ID = "INBOX"
    }
}
