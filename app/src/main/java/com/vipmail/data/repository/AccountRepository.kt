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
    private val emailAccountDao: EmailAccountDao
) {

    fun observeAccounts(): Flow<List<EmailAccount>> = emailAccountDao.observeAccounts()

    fun observeAccount(id: Long): Flow<EmailAccount?> = emailAccountDao.observeAccount(id)

    suspend fun upsertAccount(account: EmailAccount): Long =
        withContext(Dispatchers.IO) { emailAccountDao.upsert(account) }

    suspend fun deleteAccount(account: EmailAccount) =
        withContext(Dispatchers.IO) { emailAccountDao.delete(account) }

    suspend fun getFirstAccount(): EmailAccount? =
        withContext(Dispatchers.IO) { emailAccountDao.getFirstAccount() }
}

