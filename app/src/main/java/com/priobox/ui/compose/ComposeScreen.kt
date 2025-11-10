package com.priobox.ui.compose

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.priobox.data.model.EmailAccount
import com.priobox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    state: ComposeViewModel.State,
    onClose: () -> Unit,
    onSelectAccount: (EmailAccount) -> Unit,
    onUpdateTo: (String) -> Unit,
    onUpdateSubject: (String) -> Unit,
    onUpdateBody: (String) -> Unit,
    onSend: () -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val accountMenuExpanded = remember { mutableStateOf(false) }

    BackHandler { onClose() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compose_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.content_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onSend,
                        enabled = !state.isSending
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.compose_send)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.selectedAccount?.let { account ->
                Column {
                    Text(
                        stringResource(R.string.compose_from),
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedButton(onClick = { accountMenuExpanded.value = true }) {
                        Text(account.emailAddress)
                    }
                    DropdownMenu(
                        expanded = accountMenuExpanded.value,
                        onDismissRequest = { accountMenuExpanded.value = false }
                    ) {
                        state.accounts.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.emailAddress) },
                                onClick = {
                                    accountMenuExpanded.value = false
                                    onSelectAccount(option)
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.to,
                onValueChange = onUpdateTo,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.compose_to)) },
                placeholder = { Text("recipient@example.com") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.None
                )
            )

            OutlinedTextField(
                value = state.subject,
                onValueChange = onUpdateSubject,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.compose_subject)) }
            )

            OutlinedTextField(
                value = state.body,
                onValueChange = onUpdateBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                label = { Text(stringResource(R.string.compose_body)) },
                minLines = 8
            )

            Button(
                onClick = onSend,
                enabled = !state.isSending,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isSending) {
                        stringResource(R.string.compose_sending)
                    } else {
                        stringResource(R.string.compose_send)
                    }
                )
            }
        }
    }
}

