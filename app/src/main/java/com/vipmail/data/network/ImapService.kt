package com.vipmail.data.network

import android.util.Log
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPStore
import com.vipmail.data.model.EmailAccount
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

    suspend fun fetchInbox(account: EmailAccount, maxCount: Int = 50): List<EmailMessage> =
        withContext(Dispatchers.IO) {
            val properties = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", account.imapServer)
                put("mail.imaps.port", account.imapPort.toString())
                put("mail.imaps.ssl.enable", "true")
            }

            val session = Session.getInstance(properties, null)

            try {
                val store = session.getStore("imaps") as IMAPStore
                store.connect(account.username, account.password)

                val folder = store.getFolder("INBOX") as IMAPFolder
                folder.open(IMAPFolder.READ_ONLY)

                val count = folder.messageCount
                if (count <= 0) {
                    folder.close(false)
                    store.close()
                    return@withContext emptyList<EmailMessage>()
                }

                val start = (count - maxCount + 1).coerceAtLeast(1)
                val messages = folder.getMessages(start, count)
                val parsed = messages
                    .filterNotNull()
                    .sortedByDescending(Message::getReceivedDate)
                    .mapNotNull { message ->
                        val uid = folder.getUID(message)?.toString()
                        mailParser.parseMessage(account, message, uid)
                    }

                folder.close(false)
                store.close()
                parsed
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to fetch inbox", t)
                emptyList()
            }
        }

    companion object {
        private const val TAG = "ImapService"
    }
}

