package com.priobox.domain.usecase

import com.priobox.data.model.EmailAccount
import com.priobox.data.repository.MailRepository
import javax.inject.Inject

class FetchEmailsUseCase @Inject constructor(
    private val mailRepository: MailRepository
) {
    suspend operator fun invoke(account: EmailAccount) = mailRepository.syncInbox(account)
}

