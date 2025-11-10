package com.vipmail.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.EmailMessage
import com.vipmail.data.repository.AccountRepository
import com.vipmail.data.repository.MailRepository
import com.vipmail.domain.usecase.FetchEmailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val mailRepository: MailRepository,
    private val fetchEmailsUseCase: FetchEmailsUseCase
) : ViewModel() {

    sealed interface Action {
        data object OpenCompose : Action
        data object OpenSettings : Action
        data object NavigateBack : Action
        data class OpenVip(val accountId: Long) : Action
        data class SelectAccount(val account: EmailAccount) : Action
        data class Refresh(val account: EmailAccount) : Action
        data class ToggleVip(val accountId: Long, val email: String) : Action
    }

    data class State(
        val isLoading: Boolean = false,
        val accounts: List<EmailAccount> = emptyList(),
        val selectedAccount: EmailAccount? = null,
        val messages: List<EmailMessage> = emptyList(),
        val error: String? = null
    )

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<State> = combine(
        loading,
        accountRepository.observeAccounts(),
        selectedAccountId,
        selectedAccountId.flatMapLatest { accountId ->
            if (accountId == null) {
                flowOf(emptyList())
            } else {
                mailRepository.observeInbox(accountId)
            }
        },
        error
    ) { loadingState, accounts, accountId, messages, errorState ->
        val selected = when {
            accounts.isEmpty() -> null
            accountId == null -> accounts.first()
            else -> accounts.find { it.id == accountId } ?: accounts.first()
        }
        State(
            isLoading = loadingState,
            accounts = accounts,
            selectedAccount = selected,
            messages = messages,
            error = errorState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State()
    )

    init {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                if (accounts.isNotEmpty() && selectedAccountId.value == null) {
                    selectedAccountId.value = accounts.first().id
                    refresh(accounts.first())
                }
            }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.SelectAccount -> {
                selectedAccountId.value = action.account.id
                refresh(action.account)
            }
            is Action.Refresh -> refresh(action.account)
            is Action.ToggleVip -> toggleVip(action.accountId, action.email)
            else -> Unit
        }
    }

    private fun refresh(account: EmailAccount) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { fetchEmailsUseCase(account) }
                .onFailure { throwable -> error.value = throwable.message }
            loading.value = false
        }
    }

    private fun toggleVip(accountId: Long, email: String) {
        viewModelScope.launch {
            runCatching { mailRepository.toggleVip(accountId, email) }
        }
    }
}

