package com.vipmail.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    state: SettingsViewModel.AccountEditorState,
    onClose: () -> Unit,
    onUpdate: (SettingsViewModel.AccountEditorState) -> Unit,
    onSave: (SettingsViewModel.AccountEditorState) -> Unit
) {
    var formState by remember(state) { mutableStateOf(state) }

    fun update(block: SettingsViewModel.AccountEditorState.() -> SettingsViewModel.AccountEditorState) {
        formState = formState.block()
        onUpdate(formState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (formState.id == null) "Add Account" else "Edit Account")
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = formState.displayName,
                onValueChange = { update { copy(displayName = it) } },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.emailAddress,
                onValueChange = { update { copy(emailAddress = it) } },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("IMAP Settings")

            OutlinedTextField(
                value = formState.imapServer,
                onValueChange = { update { copy(imapServer = it) } },
                label = { Text("IMAP Server") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.imapPort,
                onValueChange = { update { copy(imapPort = it) } },
                label = { Text("IMAP Port") },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text("SMTP Settings")

            OutlinedTextField(
                value = formState.smtpServer,
                onValueChange = { update { copy(smtpServer = it) } },
                label = { Text("SMTP Server") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.smtpPort,
                onValueChange = { update { copy(smtpPort = it) } },
                label = { Text("SMTP Port") },
                keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.username,
                onValueChange = { update { copy(username = it) } },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.password,
                onValueChange = { update { copy(password = it) } },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Signature")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable signature")
                Switch(
                    checked = formState.signatureEnabled,
                    onCheckedChange = { isEnabled -> update { copy(signatureEnabled = isEnabled) } }
                )
            }

            OutlinedTextField(
                value = formState.signature,
                onValueChange = { update { copy(signature = it) } },
                label = { Text("Signature Text") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                minLines = 3
            )

            Button(
                onClick = {
                    onUpdate(formState)
                    onSave(formState)
                    onClose()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

