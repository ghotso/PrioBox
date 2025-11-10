package com.vipmail.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.MailSecurity
import com.vipmail.data.repository.AccountRepository
import com.vipmail.data.repository.MailRepository
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
    private val accountRepository: AccountRepository,
    private val mailRepository: MailRepository
) : ViewModel() {

    data class AccountEditorState(
        val id: Long? = null,
        val displayName: String = "",
        val emailAddress: String = "",
        val imapServer: String = "",
        val imapPort: String = "993",
        val imapSecurity: MailSecurity = MailSecurity.SSL_TLS,
        val smtpServer: String = "",
        val smtpPort: String = "465",
        val smtpSecurity: MailSecurity = MailSecurity.STARTTLS,
        val username: String = "",
        val password: String = "",
        val signature: String = "",
        val signatureEnabled: Boolean = true
    )

    data class ConnectionTestState(
        val isLoading: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )

    data class State(
        val accounts: List<EmailAccount> = emptyList(),
        val accountEditorState: AccountEditorState = AccountEditorState(),
        val imapTestState: ConnectionTestState = ConnectionTestState(),
        val smtpTestState: ConnectionTestState = ConnectionTestState()
    )

    private val editorState = MutableStateFlow(AccountEditorState())
    private val imapTestState = MutableStateFlow(ConnectionTestState())
    private val smtpTestState = MutableStateFlow(ConnectionTestState())

    val state: StateFlow<State> = combine(
        accountRepository.observeAccounts(),
        editorState,
        imapTestState,
        smtpTestState
    ) { accounts, editor, imapState, smtpState ->
        State(
            accounts = accounts,
            accountEditorState = editor,
            imapTestState = imapState,
            smtpTestState = smtpState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State()
    )

    fun startCreateAccount() {
        editorState.value = AccountEditorState()
        resetConnectionTests()
    }

    fun startEditAccount(account: EmailAccount) {
        editorState.value = AccountEditorState(
            id = account.id,
            displayName = account.displayName,
            emailAddress = account.emailAddress,
            imapServer = account.imapServer,
            imapPort = account.imapPort.toString(),
            imapSecurity = account.imapSecurity,
            smtpServer = account.smtpServer,
            smtpPort = account.smtpPort.toString(),
            smtpSecurity = account.smtpSecurity,
            username = account.username,
            password = "",
            signature = account.signature,
            signatureEnabled = account.signatureEnabled
        )
        resetConnectionTests()
    }

    fun onEditorChange(update: AccountEditorState) {
        editorState.value = update
        resetConnectionTests()
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
                signature = state.signature,
                signatureEnabled = state.signatureEnabled
            )
            accountRepository.upsertAccount(
                account = account,
                password = state.password.takeIf { it.isNotBlank() }
            )
            resetConnectionTests()
        }
    }

    fun testImapConnection() {
        viewModelScope.launch {
            val editor = editorState.value
            val password = resolvePassword(editor)
            if (password.isNullOrBlank()) {
                imapTestState.value = ConnectionTestState(error = "Password is required for IMAP test")
                return@launch
            }

            imapTestState.value = ConnectionTestState(isLoading = true)
            val account = editor.toEmailAccount()
            val result = mailRepository.testImapConnection(account, password)
            imapTestState.value = result.fold(
                onSuccess = { ConnectionTestState(message = "IMAP connection successful") },
                onFailure = { ConnectionTestState(error = it.localizedMessage ?: "IMAP connection failed") }
            )
        }
    }

    fun testSmtpConnection() {
        viewModelScope.launch {
            val editor = editorState.value
            val password = resolvePassword(editor)
            if (password.isNullOrBlank()) {
                smtpTestState.value = ConnectionTestState(error = "Password is required for SMTP test")
                return@launch
            }

            smtpTestState.value = ConnectionTestState(isLoading = true)
            val account = editor.toEmailAccount()
            val result = mailRepository.testSmtpConnection(account, password)
            smtpTestState.value = result.fold(
                onSuccess = { ConnectionTestState(message = "SMTP connection successful") },
                onFailure = { ConnectionTestState(error = it.localizedMessage ?: "SMTP connection failed") }
            )
        }
    }

    private suspend fun resolvePassword(state: AccountEditorState): String? {
        return state.password.takeIf { it.isNotBlank() } ?: state.id?.let { accountRepository.getPassword(it) }
    }

    private fun resetConnectionTests() {
        imapTestState.value = ConnectionTestState()
        smtpTestState.value = ConnectionTestState()
    }

    private fun AccountEditorState.toEmailAccount(): EmailAccount =
        EmailAccount(
            id = this.id ?: 0,
            displayName = this.displayName,
            emailAddress = this.emailAddress,
            imapServer = this.imapServer,
            imapPort = this.imapPort.toIntOrNull() ?: 993,
            imapSecurity = this.imapSecurity,
            smtpServer = this.smtpServer,
            smtpPort = this.smtpPort.toIntOrNull() ?: 465,
            smtpSecurity = this.smtpSecurity,
            username = this.username,
            signature = this.signature,
            signatureEnabled = this.signatureEnabled
        )
}

