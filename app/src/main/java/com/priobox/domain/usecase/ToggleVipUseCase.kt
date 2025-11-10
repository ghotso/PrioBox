package com.priobox.domain.usecase

import com.priobox.data.repository.MailRepository
import javax.inject.Inject

class ToggleVipUseCase @Inject constructor(
    private val mailRepository: MailRepository
) {
    suspend operator fun invoke(accountId: Long, email: String): Boolean =
        mailRepository.toggleVip(accountId, email)
}

