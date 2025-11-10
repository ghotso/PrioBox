package com.vipmail.data.repository

import com.vipmail.data.db.dao.EmailMessageDao
import com.vipmail.data.db.dao.VipSenderDao
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.EmailMessage
import com.vipmail.data.model.VipSender
import com.vipmail.data.network.ImapService
import com.vipmail.data.network.SmtpService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MailRepository @Inject constructor(
    private val emailMessageDao: EmailMessageDao,
    private val vipSenderDao: VipSenderDao,
    private val imapService: ImapService,
    private val smtpService: SmtpService,
    private val credentialStorage: CredentialStorage
) {

    fun observeInbox(accountId: Long): Flow<List<EmailMessage>> =
        combine(
            emailMessageDao.observeMessages(accountId),
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

    suspend fun syncInbox(account: EmailAccount) = withContext(Dispatchers.IO) {
        val password = credentialStorage.getPassword(account.id)
            ?: throw IllegalStateException("Credentials missing for ${account.emailAddress}")

        val vipSenders = vipSenderDao.observeVipSenders(account.id).first()
        val vipSet = vipSenders.map { it.emailAddress.lowercase() }.toSet()

        val remoteMessages = imapService.fetchInbox(account, password).map { message ->
            val sender = message.sender.lowercase()
            message.copy(isVip = vipSet.contains(sender))
        }

        emailMessageDao.replaceMessages(account.id, remoteMessages)
    }

    suspend fun sendEmail(
        account: EmailAccount,
        to: List<String>,
        subject: String,
        body: String
    ) {
        val finalBody = if (account.signatureEnabled && account.signature.isNotBlank()) {
            buildString {
                append(body.trimEnd())
                appendLine()
                appendLine()
                append(account.signature)
            }
        } else {
            body
        }

        val password = credentialStorage.getPassword(account.id)
            ?: throw IllegalStateException("Missing credentials for account ${account.emailAddress}")

        smtpService.sendEmail(account, password, to, subject, finalBody)
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

    suspend fun markMessageRead(messageId: Long) =
        emailMessageDao.updateReadState(messageId, true)
}

