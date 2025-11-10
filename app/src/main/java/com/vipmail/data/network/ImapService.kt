package com.vipmail.data.network

import android.util.Log
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.MailSecurity
import com.vipmail.data.model.EmailMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.mail.Message
import javax.mail.Session

class ImapService @Inject constructor(
    private val mailParser: MailParser
) {

    suspend fun fetchInbox(
        account: EmailAccount,
        password: String,
        maxCount: Int = 50
    ): List<EmailMessage> =
        withContext(Dispatchers.IO) {
            val properties = Properties().apply {
                when (account.imapSecurity) {
                    MailSecurity.SSL_TLS -> {
                        put("mail.store.protocol", "imaps")
                        put("mail.imaps.host", account.imapServer)
                        put("mail.imaps.port", account.imapPort.toString())
                        put("mail.imaps.ssl.enable", "true")
                    }
                    MailSecurity.STARTTLS -> {
                        put("mail.store.protocol", "imap")
                        put("mail.imap.host", account.imapServer)
                        put("mail.imap.port", account.imapPort.toString())
                        put("mail.imap.starttls.enable", "true")
                        put("mail.imap.ssl.enable", "false")
                    }
                    MailSecurity.NONE -> {
                        put("mail.store.protocol", "imap")
                        put("mail.imap.host", account.imapServer)
                        put("mail.imap.port", account.imapPort.toString())
                        put("mail.imap.starttls.enable", "false")
                        put("mail.imap.ssl.enable", "false")
                    }
                }
            }

            val session = Session.getInstance(properties, null)

            var store: IMAPStore? = null
            var folder: IMAPFolder? = null
            try {
                val protocol = when (account.imapSecurity) {
                    MailSecurity.SSL_TLS -> "imaps"
                    else -> "imap"
                }
                store = session.getStore(protocol) as IMAPStore
                store.connect(account.username, password)

                folder = store.getFolder("INBOX") as IMAPFolder
                folder.open(IMAPFolder.READ_ONLY)

                val count = folder.messageCount
                if (count <= 0) {
                    return@withContext emptyList<EmailMessage>()
                }

                val start = (count - maxCount + 1).coerceAtLeast(1)
                val messages = folder.getMessages(start, count)
                return@withContext messages
                    .filterNotNull()
                    .sortedByDescending(Message::getReceivedDate)
                    .mapNotNull { message ->
                        val uid = folder.getUID(message)
                            .takeIf { it != -1L }
                            ?.toString()
                        mailParser.parseMessage(account, message, uid)
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to fetch inbox", t)
                throw t
            } finally {
                runCatching { folder?.close(false) }
                runCatching { store?.close() }
            }
        }

    companion object {
        private const val TAG = "ImapService"
    }

    suspend fun testConnection(account: EmailAccount, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
        runCatching {
            val properties = Properties().apply {
                when (account.imapSecurity) {
                    MailSecurity.SSL_TLS -> {
                        put("mail.store.protocol", "imaps")
                        put("mail.imaps.host", account.imapServer)
                        put("mail.imaps.port", account.imapPort.toString())
                        put("mail.imaps.ssl.enable", "true")
                    }
                    MailSecurity.STARTTLS -> {
                        put("mail.store.protocol", "imap")
                        put("mail.imap.host", account.imapServer)
                        put("mail.imap.port", account.imapPort.toString())
                        put("mail.imap.starttls.enable", "true")
                        put("mail.imap.ssl.enable", "false")
                    }
                    MailSecurity.NONE -> {
                        put("mail.store.protocol", "imap")
                        put("mail.imap.host", account.imapServer)
                        put("mail.imap.port", account.imapPort.toString())
                        put("mail.imap.starttls.enable", "false")
                        put("mail.imap.ssl.enable", "false")
                    }
                }
            }

            val session = Session.getInstance(properties, null)
            val protocol = when (account.imapSecurity) {
                MailSecurity.SSL_TLS -> "imaps"
                else -> "imap"
            }
            val store = session.getStore(protocol) as IMAPStore
            store.connect(account.username, password)
            store.close()
            }
        }
}

