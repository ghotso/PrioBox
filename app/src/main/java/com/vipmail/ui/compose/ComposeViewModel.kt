package com.vipmail.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.repository.AccountRepository
import com.vipmail.domain.usecase.SendEmailUseCase
import com.vipmail.utils.toEmailList
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

    val state: StateFlow<State> = combine(
        accountRepository.observeAccounts(),
        selectedAccountId,
        to,
        subject,
        body,
        isSending,
        error
    ) { accounts, accountId, toValue, subjectValue, bodyValue, sending, errorValue ->
        val selected = when {
            accounts.isEmpty() -> null
            accountId == null -> accounts.first()
            else -> accounts.find { it.id == accountId } ?: accounts.first()
        }
        State(
            accounts = accounts,
            selectedAccount = selected,
            to = toValue,
            subject = subjectValue,
            body = bodyValue,
            isSending = sending,
            error = errorValue
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

