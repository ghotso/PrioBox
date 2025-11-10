package com.priobox.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.priobox.data.model.EmailFolder
import com.priobox.data.repository.AccountRepository
import com.priobox.data.repository.MailRepository
import com.priobox.domain.usecase.FetchEmailsUseCase
import com.priobox.utils.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
@HiltWorker
class ImapSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val accountRepository: AccountRepository,
    private val fetchEmailsUseCase: FetchEmailsUseCase,
    private val mailRepository: MailRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val accounts = accountRepository.observeAccounts()
                .firstOrNull()
                .orEmpty()
            accounts.forEach { account ->
                val existingVipUids = mailRepository.getVipMessages(account.id)
                    .map { it.uid }
                    .toSet()
                fetchEmailsUseCase(account, EmailFolder.INBOX_SERVER_ID)
                val updatedVipMessages = mailRepository.getVipMessages(account.id)
                updatedVipMessages
                    .filter { it.uid !in existingVipUids }
                    .forEach { notificationHelper.notifyVipEmail(it) }
            }
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

