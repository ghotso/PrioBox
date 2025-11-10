package com.vipmail.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "email_accounts")
data class EmailAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val emailAddress: String,
    val imapServer: String,
    val imapPort: Int,
    val smtpServer: String,
    val smtpPort: Int,
    val username: String,
    val password: String,
    val signature: String = "",
    val signatureEnabled: Boolean = true
)

