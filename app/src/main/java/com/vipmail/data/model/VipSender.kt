package com.priobox.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vip_senders",
    indices = [
        Index(value = ["accountId", "emailAddress"], unique = true)
    ]
)
data class VipSender(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val emailAddress: String
)

