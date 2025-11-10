package com.priobox.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.priobox.data.model.EmailAccount
import com.priobox.data.repository.AccountRepository
import com.priobox.domain.usecase.SendEmailUseCase
import com.priobox.utils.toEmailList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
        val body: String = "",
        val isSending: Boolean = false,
        val error: String? = null
    )

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val to = MutableStateFlow("")
    private val subject = MutableStateFlow("")
    private val body = MutableStateFlow("")
    private val isSending = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private data class FormState(
        val to: String,
        val subject: String,
        val body: String,
        val isSending: Boolean,
        val error: String?
    )

    private val formState = combine(
        to,
        subject,
        body,
        isSending,
        error
    ) { toValue, subjectValue, bodyValue, sending, errorValue ->
        FormState(
            to = toValue,
            subject = subjectValue,
            body = bodyValue,
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
            body = form.body,
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

    fun updateBody(value: String) {
        body.value = value
    }

    fun selectAccount(account: EmailAccount) {
        selectedAccountId.value = account.id
    }

    fun send(onComplete: (Action) -> Unit) {
        val account = state.value.selectedAccount ?: return
        val recipients = state.value.to.toEmailList()
        if (recipients.isEmpty()) {
            error.value = "Recipient is required"
            return
        }
        viewModelScope.launch {
            isSending.value = true
            error.value = null
            runCatching {
                sendEmailUseCase(
                    account = account,
                    to = recipients,
                    subject = state.value.subject,
                    body = state.value.body
                )
            }.onSuccess {
                to.value = ""
                subject.value = ""
                body.value = ""
                onComplete(Action.EmailSent())
            }.onFailure {
                error.value = it.message
            }
            isSending.value = false
        }
    }
}

