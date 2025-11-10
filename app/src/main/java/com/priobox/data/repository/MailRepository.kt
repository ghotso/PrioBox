package com.priobox.data.repository

import android.content.Context
import android.text.Html
import android.util.Log
import androidx.core.text.HtmlCompat
import com.priobox.data.db.dao.EmailAccountDao
import com.priobox.data.db.dao.EmailFolderDao
import com.priobox.data.db.dao.EmailMessageDao
import com.priobox.data.db.dao.VipSenderDao
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailAttachment
import com.priobox.data.model.EmailFolder
import com.priobox.data.model.EmailMessage
import com.priobox.data.model.VipSender
import com.priobox.data.network.ImapService
import com.priobox.data.network.SmtpService
import com.priobox.data.network.SmtpService.AttachmentPayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailRepository @Inject constructor(
    private val emailAccountDao: EmailAccountDao,
    private val emailMessageDao: EmailMessageDao,
    private val emailFolderDao: EmailFolderDao,
    private val vipSenderDao: VipSenderDao,
    private val imapService: ImapService,
    private val smtpService: SmtpService,
    private val credentialStorage: CredentialStorage,
    @ApplicationContext private val context: Context
) {

    fun observeFolderMessages(accountId: Long, folderServerId: String): Flow<List<EmailMessage>> =
        combine(
            emailMessageDao.observeMessages(accountId, folderServerId),
            vipSenderDao.observeVipSenders(accountId)
        ) { messages, vipSenders ->
            val vipSet = vipSenders.map { it.emailAddress.lowercase() }.toSet()
            messages.map { it.copy(isVip = vipSet.contains(it.sender.lowercase())) }
        }.flowOn(Dispatchers.IO)

    fun observeVipInbox(accountId: Long): Flow<List<EmailMessage>> =
        combine(
            emailMessageDao.observeVipMessages(accountId),
            vipSenderDao.observeVipSenders(accountId)
        ) { messages, vipSenders ->
            val vipSet = vipSenders.map { it.emailAddress.lowercase() }.toSet()
            messages.map { it.copy(isVip = vipSet.contains(it.sender.lowercase())) }
        }.flowOn(Dispatchers.IO)

    suspend fun syncFolder(account: EmailAccount, folderServerId: String) = withContext(Dispatchers.IO) {
        val password = credentialStorage.getPassword(account.id)
            ?: throw IllegalStateException("Credentials missing for ${account.emailAddress}")

        val remoteMessages = imapService.fetchMessages(account, password, folderServerId).map {
            it.copy(folder = folderServerId)
        }

        emailMessageDao.replaceMessages(account.id, folderServerId, remoteMessages)
    }

    suspend fun syncFolders(account: EmailAccount) = withContext(Dispatchers.IO) {
        val password = credentialStorage.getPassword(account.id)
            ?: throw IllegalStateException("Credentials missing for ${account.emailAddress}")
        val remoteFolders = imapService.fetchFolders(account, password)
        emailFolderDao.replaceFolders(account.id, remoteFolders)
    }

    fun observeFolders(accountId: Long): Flow<List<EmailFolder>> =
        emailFolderDao.observeFolders(accountId)

    suspend fun sendEmail(
        account: EmailAccount,
        to: List<String>,
        subject: String,
        bodyHtml: String,
        attachments: List<EmailAttachment>
    ) = withContext(Dispatchers.IO) {
        val htmlWithSignature = if (account.signatureEnabled && account.signature.isNotBlank()) {
            val escapedSignature = Html.escapeHtml(account.signature).replace(\"\\n\", \"<br/>\")
            buildString {
                append(bodyHtml.trimEnd())
                append(\"<br/><br/>\")
                append(escapedSignature)
            }
        } else {
            bodyHtml
        }

        val password = credentialStorage.getPassword(account.id)
            ?: throw IllegalStateException(\"Missing credentials for account ${account.emailAddress}\")

        val bodyText = HtmlCompat.fromHtml(htmlWithSignature, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()

        val payloads = attachments.mapNotNull { attachment ->
            val bytes = attachment.data ?: attachment.uri?.let { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } catch (exception: IOException) {
                    Log.e(TAG, \"Failed to read attachment ${attachment.displayName}\", exception)
                    null
                }
            }

            val contentBytes = bytes ?: run {
                Log.w(TAG, \"Skipping attachment ${attachment.displayName}: no data available\")
                null
            }

            contentBytes?.let {
                AttachmentPayload(
                    fileName = attachment.displayName,
                    mimeType = attachment.mimeType,
                    bytes = it,
                    inline = attachment.inline,
                    contentId = attachment.contentId
                )
            }
        }

        smtpService.sendEmail(
            account = account,
            password = password,
            to = to,
            subject = subject,
            bodyHtml = htmlWithSignature,
            bodyText = bodyText,
            attachments = payloads
        )
    }

    suspend fun toggleVip(accountId: Long, email: String): Boolean {
        val existing = vipSenderDao.getVipSender(accountId, email)
        return if (existing == null) {
            vipSenderDao.upsert(VipSender(accountId = accountId, emailAddress = email))
            emailMessageDao.updateVipStatus(accountId, email, true)
            true
        } else {
            vipSenderDao.delete(existing)
            emailMessageDao.updateVipStatus(accountId, email, false)
            false
        }
    }

    fun observeVipSenders(accountId: Long) = vipSenderDao.observeVipSenders(accountId)

    suspend fun getVipMessages(accountId: Long): List<EmailMessage> =
        emailMessageDao.getVipMessages(accountId)

    suspend fun testImapConnection(account: EmailAccount, password: String): Result<Unit> =
        imapService.testConnection(account, password)

    suspend fun testSmtpConnection(account: EmailAccount, password: String): Result<Unit> =
        smtpService.testConnection(account, password)

    fun observeMessage(messageId: Long): Flow<EmailMessage?> =
        emailMessageDao.observeMessage(messageId)

    suspend fun setMessageReadState(messageId: Long, isRead: Boolean) = withContext(Dispatchers.IO) {
        val message = emailMessageDao.getMessage(messageId) ?: return@withContext
        emailMessageDao.updateReadState(messageId, isRead)

        runCatching {
            val account = emailAccountDao.getAccount(message.accountId) ?: return@runCatching
            val password = credentialStorage.getPassword(account.id) ?: return@runCatching
            imapService.updateMessageReadState(account, password, message.folder, message.uid, isRead)
        }.onFailure {
            Log.e(TAG, "Failed to update remote read state for message $messageId", it)
        }
    }

    suspend fun markMessageRead(messageId: Long) = setMessageReadState(messageId, true)

    companion object {
        private const val TAG = "MailRepository"
    }
}

