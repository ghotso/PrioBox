package com.priobox.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailFolder
import com.priobox.data.model.EmailMessage
import com.priobox.data.repository.AccountRepository
import com.priobox.data.repository.MailRepository
import com.priobox.domain.usecase.FetchEmailsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
        data class OpenMessage(val messageId: Long) : Action
        data class SelectFolder(val folderServerId: String) : Action
        data class SetMessageRead(val messageId: Long, val isRead: Boolean) : Action
        data object CreateFirstAccount : Action
    }

    private data class AccountStreams(
        val loading: Boolean,
        val accounts: List<EmailAccount>,
        val selectedAccountId: Long?
    )

    private data class FolderStreams(
        val folders: List<EmailFolder>,
        val selectedFolderId: String
    )

    private data class CombinedStreams(
        val loading: Boolean,
        val accounts: List<EmailAccount>,
        val selectedAccountId: Long?,
        val folders: List<EmailFolder>,
        val selectedFolderId: String,
        val messages: List<EmailMessage>
    )

    data class State(
        val isLoading: Boolean = false,
        val accounts: List<EmailAccount> = emptyList(),
        val selectedAccount: EmailAccount? = null,
        val folders: List<EmailFolder> = emptyList(),
        val selectedFolder: EmailFolder? = null,
        val messages: List<EmailMessage> = emptyList(),
        val error: String? = null
    )

    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val selectedFolderId = MutableStateFlow(EmailFolder.INBOX_SERVER_ID)
    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val foldersFlow = selectedAccountId.flatMapLatest { accountId ->
        if (accountId == null) {
            flowOf(emptyList())
        } else {
            mailRepository.observeFolders(accountId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val messagesFlow = combine(selectedAccountId, selectedFolderId) { accountId, folderId ->
        accountId to folderId
    }.flatMapLatest { (accountId, folderId) ->
        if (accountId == null || folderId.isBlank()) {
            flowOf(emptyList())
        } else {
            mailRepository.observeFolderMessages(accountId, folderId)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val accountStreams = combine(
        loading,
        accountRepository.observeAccounts(),
        selectedAccountId
    ) { loadingState, accounts, accountId ->
        AccountStreams(
            loading = loadingState,
            accounts = accounts,
            selectedAccountId = accountId
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val folderStreams = combine(
        foldersFlow,
        selectedFolderId
    ) { folders, folderId ->
        FolderStreams(
            folders = folders,
            selectedFolderId = folderId
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val combinedStreams = combine(
        accountStreams,
        folderStreams,
        messagesFlow
    ) { accountData, folderData, messages ->
        CombinedStreams(
            loading = accountData.loading,
            accounts = accountData.accounts,
            selectedAccountId = accountData.selectedAccountId,
            folders = folderData.folders,
            selectedFolderId = folderData.selectedFolderId,
            messages = messages
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<State> = combine(
        combinedStreams,
        error
    ) { data, errorState ->
        val selectedAccount = when {
            data.accounts.isEmpty() -> null
            data.selectedAccountId == null -> data.accounts.first()
            else -> data.accounts.find { it.id == data.selectedAccountId } ?: data.accounts.first()
        }

        val selectedFolder = data.folders.firstOrNull { it.serverId.equals(data.selectedFolderId, ignoreCase = true) }
            ?: data.folders.firstOrNull { it.serverId.equals(EmailFolder.INBOX_SERVER_ID, ignoreCase = true) }
            ?: data.folders.firstOrNull()

        State(
            isLoading = data.loading,
            accounts = data.accounts,
            selectedAccount = selectedAccount,
            folders = data.folders,
            selectedFolder = selectedFolder,
            messages = data.messages,
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
                    val initialAccount = accounts.first()
                    selectedAccountId.value = initialAccount.id
                    selectedFolderId.value = EmailFolder.INBOX_SERVER_ID
                    refreshFolders(initialAccount)
                    refresh(initialAccount)
                }
            }
        }

        viewModelScope.launch {
            combine(
                selectedAccountId.filterNotNull(),
                foldersFlow
            ) { _, folders -> folders }
                .collect { folders ->
                    if (folders.isEmpty()) return@collect
                    val currentSelection = selectedFolderId.value
                    val resolved = folders.firstOrNull { it.serverId.equals(currentSelection, ignoreCase = true) }
                        ?: folders.firstOrNull { it.serverId.equals(EmailFolder.INBOX_SERVER_ID, ignoreCase = true) }
                        ?: folders.first()
                    if (!resolved.serverId.equals(currentSelection, ignoreCase = true)) {
                        selectedFolderId.value = resolved.serverId
                    }
                }
        }
    }

    fun onAction(action: Action) {
        when (action) {
            is Action.SelectAccount -> {
                selectedAccountId.value = action.account.id
                selectedFolderId.value = EmailFolder.INBOX_SERVER_ID
                refreshFolders(action.account)
                refresh(action.account)
            }

            is Action.SelectFolder -> {
                if (selectedFolderId.value != action.folderServerId) {
                    selectedFolderId.value = action.folderServerId
                }
                val accountId = selectedAccountId.value
                val account = state.value.accounts.firstOrNull { it.id == accountId }
                if (account != null) {
                    refresh(account)
                }
            }

            is Action.Refresh -> refresh(action.account)

            is Action.ToggleVip -> toggleVip(action.accountId, action.email)

            is Action.OpenMessage -> Unit

            is Action.SetMessageRead -> setMessageRead(action.messageId, action.isRead)

            else -> Unit
        }
    }

    private fun refresh(account: EmailAccount) {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val folderId = selectedFolderId.value.ifBlank { EmailFolder.INBOX_SERVER_ID }
            runCatching { fetchEmailsUseCase(account, folderId) }
                .onFailure { throwable ->
                    error.value = throwable.localizedMessage ?: "Failed to refresh mailbox"
                }
            loading.value = false
        }
    }

    private fun refreshFolders(account: EmailAccount) {
        viewModelScope.launch {
            runCatching {
                mailRepository.syncFolders(account)
            }.onFailure { throwable ->
                error.value = throwable.localizedMessage ?: "Failed to refresh folders"
            }
        }
    }

    private fun toggleVip(accountId: Long, email: String) {
        viewModelScope.launch {
            runCatching { mailRepository.toggleVip(accountId, email) }
        }
    }

    private fun setMessageRead(messageId: Long, isRead: Boolean) {
        viewModelScope.launch {
            runCatching { mailRepository.setMessageReadState(messageId, isRead) }
        }
    }
}

