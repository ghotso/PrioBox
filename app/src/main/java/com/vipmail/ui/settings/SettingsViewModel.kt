package com.vipmail.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    data class AccountEditorState(
        val id: Long? = null,
        val displayName: String = "",
        val emailAddress: String = "",
        val imapServer: String = "",
        val imapPort: String = "993",
        val smtpServer: String = "",
        val smtpPort: String = "465",
        val username: String = "",
        val password: String = "",
        val signature: String = "",
        val signatureEnabled: Boolean = true
    )

    data class State(
        val accounts: List<EmailAccount> = emptyList(),
        val accountEditorState: AccountEditorState = AccountEditorState()
    )

    private val editorState = MutableStateFlow(AccountEditorState())

    val state: StateFlow<State> = combine(
        accountRepository.observeAccounts(),
        editorState
    ) { accounts, editor ->
        State(accounts = accounts, accountEditorState = editor)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State()
    )

    fun startCreateAccount() {
        editorState.value = AccountEditorState()
    }

    fun startEditAccount(account: EmailAccount) {
        editorState.value = AccountEditorState(
            id = account.id,
            displayName = account.displayName,
            emailAddress = account.emailAddress,
            imapServer = account.imapServer,
            imapPort = account.imapPort.toString(),
            smtpServer = account.smtpServer,
            smtpPort = account.smtpPort.toString(),
            username = account.username,
            password = account.password,
            signature = account.signature,
            signatureEnabled = account.signatureEnabled
        )
    }

    fun onEditorChange(update: AccountEditorState) {
        editorState.value = update
    }

    fun onAccountSaved(state: AccountEditorState) {
        viewModelScope.launch {
            val account = EmailAccount(
                id = state.id ?: 0,
                displayName = state.displayName,
                emailAddress = state.emailAddress,
                imapServer = state.imapServer,
                imapPort = state.imapPort.toIntOrNull() ?: 993,
                smtpServer = state.smtpServer,
                smtpPort = state.smtpPort.toIntOrNull() ?: 465,
                username = state.username,
                password = state.password,
                signature = state.signature,
                signatureEnabled = state.signatureEnabled
            )
            accountRepository.upsertAccount(account)
        }
    }
}

