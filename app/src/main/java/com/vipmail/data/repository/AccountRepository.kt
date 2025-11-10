package com.vipmail.data.repository

import com.vipmail.data.db.dao.EmailAccountDao
import com.vipmail.data.model.EmailAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val emailAccountDao: EmailAccountDao,
    private val credentialStorage: CredentialStorage
) {

    fun observeAccounts(): Flow<List<EmailAccount>> = emailAccountDao.observeAccounts()

    fun observeAccount(id: Long): Flow<EmailAccount?> = emailAccountDao.observeAccount(id)

    suspend fun upsertAccount(account: EmailAccount, password: String?): Long =
        withContext(Dispatchers.IO) {
            if (account.id == 0L && password.isNullOrBlank()) {
                throw IllegalArgumentException("Password required for new account")
            }
            val savedId = if (account.id == 0L) {
                emailAccountDao.upsert(account)
            } else {
                emailAccountDao.update(account)
                account.id
            }
            password?.takeIf { it.isNotBlank() }?.let {
                credentialStorage.setPassword(savedId, it)
            }
            savedId
        }

    suspend fun deleteAccount(account: EmailAccount) =
        withContext(Dispatchers.IO) {
            emailAccountDao.delete(account)
            credentialStorage.clearPassword(account.id)
        }

    suspend fun getFirstAccount(): EmailAccount? =
        withContext(Dispatchers.IO) { emailAccountDao.getFirstAccount() }

    suspend fun getPassword(accountId: Long): String? =
        withContext(Dispatchers.IO) { credentialStorage.getPassword(accountId) }
}

