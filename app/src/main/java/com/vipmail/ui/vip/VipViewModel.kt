package com.priobox.ui.vip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.VipSender
import com.priobox.data.repository.AccountRepository
import com.priobox.data.repository.MailRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class VipViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val mailRepository: MailRepository
) : ViewModel() {

    sealed interface Action {
        data object NavigateBack : Action
    }

    data class State(
        val accounts: List<EmailAccount> = emptyList(),
        val selectedAccount: EmailAccount? = null,
        val vipSenders: List<VipSender> = emptyList(),
        val newVipEmail: String = "",
        val error: String? = null
    )

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val newVipEmail = MutableStateFlow("")
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<State> = combine(
        accountRepository.observeAccounts(),
        selectedAccountId,
        selectedAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList())
            else mailRepository.observeVipSenders(accountId)
        },
        newVipEmail,
        error
    ) { accounts, accountId, vipSenders, newEmail, errorState ->
        val selected = when {
            accounts.isEmpty() -> null
            accountId == null -> accounts.first()
            else -> accounts.find { it.id == accountId } ?: accounts.first()
        }
        State(
            accounts = accounts,
            selectedAccount = selected,
            vipSenders = vipSenders,
            newVipEmail = newEmail,
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
                }
            }
        }
    }

    fun selectAccount(account: EmailAccount) {
        selectedAccountId.value = account.id
    }

    fun updateNewVipEmail(value: String) {
        newVipEmail.value = value
    }

    fun addVip() {
        val account = state.value.selectedAccount ?: return
        val email = state.value.newVipEmail.trim()
        if (email.isBlank()) {
            error.value = "Email cannot be empty"
            return
        }
        viewModelScope.launch {
            runCatching { mailRepository.toggleVip(account.id, email) }
                .onSuccess { added ->
                    if (added) newVipEmail.value = ""
                }
                .onFailure { error.value = it.message }
        }
    }

    fun removeVip(vip: VipSender) {
        viewModelScope.launch {
            runCatching { mailRepository.toggleVip(vip.accountId, vip.emailAddress) }
        }
    }
}

