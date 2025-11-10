package com.vipmail.domain.usecase

import com.vipmail.data.model.EmailAccount
import com.vipmail.data.repository.MailRepository
import javax.inject.Inject

class FetchEmailsUseCase @Inject constructor(
    private val mailRepository: MailRepository
) {
    suspend operator fun invoke(account: EmailAccount) = mailRepository.syncInbox(account)
}

