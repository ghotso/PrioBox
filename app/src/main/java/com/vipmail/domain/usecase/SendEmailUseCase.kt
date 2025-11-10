package com.vipmail.domain.usecase

import com.vipmail.data.model.EmailAccount
import com.vipmail.data.repository.MailRepository
import javax.inject.Inject

class SendEmailUseCase @Inject constructor(
    private val mailRepository: MailRepository
) {
    suspend operator fun invoke(
        account: EmailAccount,
        to: List<String>,
        subject: String,
        body: String
    ) = mailRepository.sendEmail(account, to, subject, body)
}

