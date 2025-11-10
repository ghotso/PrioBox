package com.priobox.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailMessage
import com.priobox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxViewModel.State,
    onAction: (InboxViewModel.Action) -> Unit
) {
    val appBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val accountsExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.selectedAccount?.displayName
                                ?: stringResource(R.string.inbox_title),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = state.selectedAccount?.emailAddress ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(InboxViewModel.Action.NavigateBack) }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.content_back)
                        )
                    }
                },
                actions = {
                    if (state.accounts.size > 1) {
                        IconButton(onClick = { accountsExpanded.value = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = stringResource(R.string.content_accounts_menu)
                            )
                        }
                        DropdownMenu(
                            expanded = accountsExpanded.value,
                            onDismissRequest = { accountsExpanded.value = false }
                        ) {
                            state.accounts.forEach { account ->
                                DropdownMenuItem(
                                    text = { Text(account.displayName) },
                                    onClick = {
                                        accountsExpanded.value = false
                                        onAction(InboxViewModel.Action.SelectAccount(account))
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onAction(InboxViewModel.Action.OpenSettings) }
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }

                    IconButton(
                        onClick = {
                            state.selectedAccount?.let {
                                onAction(InboxViewModel.Action.Refresh(it))
                            }
                        }
                    ) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.content_refresh)
                        )
                    }
                },
                scrollBehavior = appBarScrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAction(InboxViewModel.Action.OpenCompose) }) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.content_compose)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            state.selectedAccount?.let { account ->
                AssistChip(
                    onClick = { onAction(InboxViewModel.Action.OpenVip(account.id)) },
                    label = { Text(stringResource(R.string.inbox_vip_contacts)) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.accounts.isEmpty()) {
                EmptyAccountsState(
                    onAddAccount = { onAction(InboxViewModel.Action.CreateFirstAccount) }
                )
                return@Scaffold
            }

            when {
                state.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.inbox_syncing),
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }

                state.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.inbox_no_messages))
                    }
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.messages) { message ->
                            EmailRow(
                                message = message,
                                onToggleVip = {
                                    state.selectedAccount?.let { account ->
                                        onAction(
                                            InboxViewModel.Action.ToggleVip(
                                                account.id,
                                                message.sender
                                            )
                                        )
                                    }
                                },
                                onOpenMessage = {
                                    onAction(InboxViewModel.Action.OpenMessage(message.id))
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAccountsState(
    onAddAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.inbox_empty_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = stringResource(R.string.inbox_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        Button(
            onClick = onAddAccount,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(stringResource(R.string.inbox_empty_add_account))
        }
    }
}

@Composable
private fun EmailRow(
    message: EmailMessage,
    onToggleVip: () -> Unit,
    onOpenMessage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMessage() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.padding(4.dp))
                if (message.isVip) {
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.badge_vip)) }
                    )
                }
            }
            Text(
                text = message.subject,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = message.preview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        IconButton(onClick = onToggleVip) {
            Icon(
                imageVector = if (message.isVip) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(R.string.message_action_toggle_vip)
            )
        }
    }
}

