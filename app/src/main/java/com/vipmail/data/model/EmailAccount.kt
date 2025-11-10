package com.priobox.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.priobox.data.model.MailSecurity

@Entity(tableName = "email_accounts")
data class EmailAccount(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val emailAddress: String,
    val imapServer: String,
    val imapPort: Int,
    val imapSecurity: MailSecurity = MailSecurity.SSL_TLS,
    val smtpServer: String,
    val smtpPort: Int,
    val smtpSecurity: MailSecurity = MailSecurity.STARTTLS,
    val username: String,
    val signature: String = "",
    val signatureEnabled: Boolean = true
)

