package com.vipmail.ui.settings

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vipmail.data.model.MailSecurity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditScreen(
    state: SettingsViewModel.AccountEditorState,
    imapTestState: SettingsViewModel.ConnectionTestState,
    smtpTestState: SettingsViewModel.ConnectionTestState,
    onClose: () -> Unit,
    onUpdate: (SettingsViewModel.AccountEditorState) -> Unit,
    onSave: (SettingsViewModel.AccountEditorState) -> Unit,
    onTestImap: () -> Unit,
    onTestSmtp: () -> Unit
) {
    var formState by remember(state) { mutableStateOf(state) }

    fun update(block: SettingsViewModel.AccountEditorState.() -> SettingsViewModel.AccountEditorState) {
        formState = formState.block()
        onUpdate(formState)
    }

    val isNewAccount = formState.id == null
    val requiredFieldsFilled =
        formState.displayName.isNotBlank() &&
            formState.emailAddress.isNotBlank() &&
            formState.imapServer.isNotBlank() &&
            formState.imapPort.isNotBlank() &&
            formState.smtpServer.isNotBlank() &&
            formState.smtpPort.isNotBlank() &&
            formState.username.isNotBlank()
    val isPasswordProvided = !isNewAccount || formState.password.isNotBlank()
    val canTestConnection = requiredFieldsFilled && (formState.password.isNotBlank() || !isNewAccount)
    val isSaveEnabled = requiredFieldsFilled && isPasswordProvided

    BackHandler { onClose() }

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

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.imapPort,
                    onValueChange = { update { copy(imapPort = it) } },
                    label = { Text("IMAP Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                SecurityDropdown(
                    label = "IMAP Security",
                    value = formState.imapSecurity,
                    onValueSelected = { update { copy(imapSecurity = it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("SMTP Settings")

            OutlinedTextField(
                value = formState.smtpServer,
                onValueChange = { update { copy(smtpServer = it) } },
                label = { Text("SMTP Server") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.smtpPort,
                    onValueChange = { update { copy(smtpPort = it) } },
                    label = { Text("SMTP Port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                SecurityDropdown(
                    label = "SMTP Security",
                    value = formState.smtpSecurity,
                    onValueSelected = { update { copy(smtpSecurity = it) } },
                    modifier = Modifier.weight(1f)
                )
            }

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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = {
                    if (!isNewAccount) {
                        Text("Leave blank to keep existing password")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTestImap,
                        enabled = canTestConnection && !imapTestState.isLoading
                    ) {
                        Text(
                            when {
                                imapTestState.isLoading -> "Testing IMAP…"
                                else -> "Test IMAP"
                            }
                        )
                    }
                    Button(
                        onClick = onTestSmtp,
                        enabled = canTestConnection && !smtpTestState.isLoading
                    ) {
                        Text(
                            when {
                                smtpTestState.isLoading -> "Testing SMTP…"
                                else -> "Test SMTP"
                            }
                        )
                    }
                }

                imapTestState.message?.let {
                    Text(
                        text = "IMAP: $it",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                imapTestState.error?.let {
                    Text(
                        text = "IMAP: $it",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                smtpTestState.message?.let {
                    Text(
                        text = "SMTP: $it",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                smtpTestState.error?.let {
                    Text(
                        text = "SMTP: $it",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

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
                enabled = isSaveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
private fun SecurityDropdown(
    label: String,
    value: MailSecurity,
    onValueSelected: (MailSecurity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value.displayName,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = "Select $label")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            MailSecurity.values().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        expanded = false
                        onValueSelected(option)
                    }
                )
            }
        }
    }
}

