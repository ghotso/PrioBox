package com.priobox.data.network

import android.util.Log
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailFolder
import com.priobox.data.model.EmailMessage
import com.priobox.data.model.MailSecurity
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session

class ImapService @Inject constructor(
    private val mailParser: MailParser
) {

    suspend fun fetchMessages(
        account: EmailAccount,
        password: String,
        folderServerId: String,
        maxCount: Int = 50
    ): List<EmailMessage> =
        withContext(Dispatchers.IO) {
            val session = Session.getInstance(createProperties(account), null)

            var store: IMAPStore? = null
            var folder: IMAPFolder? = null
            try {
                store = session.getStore(resolveProtocol(account)) as IMAPStore
                store.connect(account.username, password)

                folder = store.getFolder(folderServerId) as? IMAPFolder
                    ?: store.getFolder(EmailFolder.INBOX_SERVER_ID) as IMAPFolder
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
                        mailParser.parseMessage(account, message, uid, folder.fullName)
                    }
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to fetch folder ${folderServerId}", t)
                throw t
            } finally {
                runCatching { folder?.close(false) }
                runCatching { store?.close() }
            }
        }

    suspend fun fetchFolders(
        account: EmailAccount,
        password: String
    ): List<EmailFolder> = withContext(Dispatchers.IO) {
        val session = Session.getInstance(createProperties(account), null)
        var store: IMAPStore? = null
        try {
            store = session.getStore(resolveProtocol(account)) as IMAPStore
            store.connect(account.username, password)

            val defaultFolder = store.defaultFolder
            val allFolders = defaultFolder.list("*")?.toList().orEmpty()
            allFolders
                .filter { folder -> folder.type and Folder.HOLDS_MESSAGES != 0 }
                .map { folder ->
                    EmailFolder(
                        accountId = account.id,
                        serverId = folder.fullName,
                        displayName = folder.displayName(),
                        selectable = folder.type and Folder.HOLDS_MESSAGES != 0,
                        typeFlags = folder.type
                    )
                }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to fetch folders", t)
            throw t
        } finally {
            runCatching { store?.close() }
        }
    }

    private fun Folder.displayName(): String {
        val full = fullName
        if (full.equals(EmailFolder.INBOX_SERVER_ID, ignoreCase = true)) {
            return "Inbox"
        }
        val nameValue = name
        if (!nameValue.isNullOrBlank()) {
            return nameValue
        }
        val separator = runCatching { separator }.getOrDefault('/')
        return if (separator != 0.toChar() && full.contains(separator)) {
            full.substringAfterLast(separator)
        } else {
            full
        }
    }

    private fun createProperties(account: EmailAccount): Properties = Properties().apply {
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

    private fun resolveProtocol(account: EmailAccount): String = when (account.imapSecurity) {
        MailSecurity.SSL_TLS -> "imaps"
        else -> "imap"
    }

    companion object {
        private const val TAG = "ImapService"
    }

    suspend fun testConnection(account: EmailAccount, password: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val session = Session.getInstance(createProperties(account), null)
                val store = session.getStore(resolveProtocol(account)) as IMAPStore
                store.connect(account.username, password)
                store.close()
            }
        }
}

