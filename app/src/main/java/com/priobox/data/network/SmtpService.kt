package com.priobox.data.network

import android.util.Log
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.MailSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Properties
import javax.inject.Inject
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource
import javax.activation.DataHandler
import java.util.UUID

class SmtpService @Inject constructor() {

    suspend fun sendEmail(
        account: EmailAccount,
        password: String,
        to: List<String>,
        subject: String,
        bodyHtml: String,
        bodyText: String,
        attachments: List<AttachmentPayload>
    ) = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.transport.protocol", "smtp")
            put("mail.smtp.host", account.smtpServer)
            put("mail.smtp.port", account.smtpPort.toString())
            put("mail.smtp.auth", "true")
            when (account.smtpSecurity) {
                MailSecurity.SSL_TLS -> {
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.starttls.enable", "false")
                }
                MailSecurity.STARTTLS -> {
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.ssl.enable", "false")
                }
                MailSecurity.NONE -> {
                    put("mail.smtp.starttls.enable", "false")
                    put("mail.smtp.ssl.enable", "false")
                }
            }
        }

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(account.username, password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(account.emailAddress))
                setRecipients(
                    Message.RecipientType.TO,
                    to.map { InternetAddress(it) }.toTypedArray()
                )
                this.subject = subject
                sentDate = Date()
            }

            val mixedMultipart = MimeMultipart("mixed")

            val alternativeMultipart = MimeMultipart("alternative").apply {
                val textPart = MimeBodyPart().apply {
                    setText(bodyText, Charsets.UTF_8.name())
                }
                val htmlPart = MimeBodyPart().apply {
                    setContent(bodyHtml, "text/html; charset=${Charsets.UTF_8.name()}")
                }
                addBodyPart(textPart)
                addBodyPart(htmlPart)
            }

            val bodyContainer = MimeBodyPart().apply {
                setContent(alternativeMultipart)
            }
            mixedMultipart.addBodyPart(bodyContainer)

            attachments.filter { it.inline }.forEach { attachment ->
                val cid = attachment.contentId ?: UUID.randomUUID().toString()
                val inlinePart = MimeBodyPart().apply {
                    dataHandler = DataHandler(ByteArrayDataSource(attachment.bytes, attachment.mimeType))
                    disposition = MimeBodyPart.INLINE
                    setHeader("Content-ID", "<$cid>")
                    fileName = attachment.fileName
                }
                mixedMultipart.addBodyPart(inlinePart)
            }

            attachments.filterNot { it.inline }.forEach { attachment ->
                val filePart = MimeBodyPart().apply {
                    dataHandler = DataHandler(ByteArrayDataSource(attachment.bytes, attachment.mimeType))
                    disposition = MimeBodyPart.ATTACHMENT
                    fileName = attachment.fileName
                }
                mixedMultipart.addBodyPart(filePart)
            }

            message.setContent(mixedMultipart)
            message.saveChanges()

            Transport.send(message)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to send email", t)
            throw t
        }
    }

    companion object {
        private const val TAG = "SmtpService"
    }

    suspend fun testConnection(
        account: EmailAccount,
        password: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val props = Properties().apply {
                put("mail.transport.protocol", "smtp")
                put("mail.smtp.host", account.smtpServer)
                put("mail.smtp.port", account.smtpPort.toString())
                put("mail.smtp.auth", "true")
                when (account.smtpSecurity) {
                    MailSecurity.SSL_TLS -> {
                        put("mail.smtp.ssl.enable", "true")
                        put("mail.smtp.starttls.enable", "false")
                    }
                    MailSecurity.STARTTLS -> {
                        put("mail.smtp.starttls.enable", "true")
                        put("mail.smtp.ssl.enable", "false")
                    }
                    MailSecurity.NONE -> {
                        put("mail.smtp.starttls.enable", "false")
                        put("mail.smtp.ssl.enable", "false")
                    }
                }
            }

            val session = Session.getInstance(props, object : javax.mail.Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(account.username, password)
                }
            })

            val transport = session.getTransport("smtp")
            transport.connect()
            transport.close()
        }
    }

    data class AttachmentPayload(
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray,
        val inline: Boolean = false,
        val contentId: String? = null
    )
}

