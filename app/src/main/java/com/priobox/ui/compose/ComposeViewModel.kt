package com.priobox.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailAttachment
import com.priobox.data.repository.AccountRepository
import com.priobox.domain.usecase.SendEmailUseCase
import com.priobox.utils.toEmailList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComposeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val sendEmailUseCase: SendEmailUseCase
) : ViewModel() {

    sealed interface Action {
        data object Close : Action
        data class EmailSent(val messageId: String? = null) : Action
    }

    data class State(
        val accounts: List<EmailAccount> = emptyList(),
        val selectedAccount: EmailAccount? = null,
        val to: String = "",
        val subject: String = "",
        val bodyHtml: String = "",
        val attachments: List<EmailAttachment> = emptyList(),
        val isSending: Boolean = false,
        val error: String? = null
    )

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val to = MutableStateFlow("")
    private val subject = MutableStateFlow("")
    private val bodyHtml = MutableStateFlow("")
    private val attachments = MutableStateFlow<List<EmailAttachment>>(emptyList())
    private val isSending = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private data class FormState(
        val to: String,
        val subject: String,
        val bodyHtml: String,
        val attachments: List<EmailAttachment>,
        val isSending: Boolean,
        val error: String?
    )

    private val formState = combine(
        to,
        subject,
        bodyHtml,
        attachments,
        isSending,
        error
    ) { toValue, subjectValue, bodyValue, attachmentsValue, sending, errorValue ->
        FormState(
            to = toValue,
            subject = subjectValue,
            bodyHtml = bodyValue,
            attachments = attachmentsValue,
            isSending = sending,
            error = errorValue
        )
    }

    val state: StateFlow<State> = combine(
        accountRepository.observeAccounts(),
        selectedAccountId,
        formState
    ) { accounts, accountId, form ->
        val selected = when {
            accounts.isEmpty() -> null
            accountId == null -> accounts.first()
            else -> accounts.find { it.id == accountId } ?: accounts.first()
        }
        State(
            accounts = accounts,
            selectedAccount = selected,
            to = form.to,
            subject = form.subject,
            bodyHtml = form.bodyHtml,
            attachments = form.attachments,
            isSending = form.isSending,
            error = form.error
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State()
    )

    init {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                if (accounts.isNotEmpty() && selectedAccountId.value == null) {
                    selectedAccountId.value = accounts.first().id
                }
            }
        }
    }

    fun updateTo(value: String) {
        to.value = value
    }

    fun updateSubject(value: String) {
        subject.value = value
    }

    fun updateBodyHtml(value: String) {
        bodyHtml.value = value
    }

    fun selectAccount(account: EmailAccount) {
        selectedAccountId.value = account.id
    }

    fun addAttachment(attachment: EmailAttachment) {
        attachments.update { current ->
            if (current.any { it.contentId != null && it.contentId == attachment.contentId && attachment.inline }) {
                current
            } else {
                current + attachment
            }
        }
    }

    fun removeAttachment(attachment: EmailAttachment) {
        attachments.update { current -> current.filterNot { it == attachment } }
    }

    fun send(onComplete: (Action) -> Unit) {
        val account = state.value.selectedAccount ?: return
        val recipients = state.value.to.toEmailList()
        if (recipients.isEmpty()) {
            error.value = "Recipient is required"
            return
        }
        val attachmentsSnapshot = state.value.attachments
        val preparedHtml = attachmentsSnapshot.fold(state.value.bodyHtml) { acc, attachment ->
            if (attachment.inline && attachment.placeholder != null && attachment.contentId != null) {
                acc.replace(attachment.placeholder, "cid:${attachment.contentId}")
            } else {
                acc
            }
        }
        viewModelScope.launch {
            isSending.value = true
            error.value = null
            runCatching {
                sendEmailUseCase(
                    account = account,
                    to = recipients,
                    subject = state.value.subject,
                    bodyHtml = preparedHtml,
                    attachments = attachmentsSnapshot
                )
            }.onSuccess {
                to.value = ""
                subject.value = ""
                bodyHtml.value = ""
                attachments.value = emptyList()
                onComplete(Action.EmailSent())
            }.onFailure {
                error.value = it.message
            }
            isSending.value = false
        }
    }
}

