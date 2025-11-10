package com.priobox.ui.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.priobox.R
import com.priobox.data.model.MailSecurity

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
    fun update(block: SettingsViewModel.AccountEditorState.() -> SettingsViewModel.AccountEditorState) {
        onUpdate(state.block())
    }

    val formState = state
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
                    Text(
                        if (formState.id == null) {
                            stringResource(R.string.account_add_title)
                        } else {
                            stringResource(R.string.account_edit_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.content_back)
                        )
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
                label = { Text(stringResource(R.string.account_display_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.emailAddress,
                onValueChange = { update { copy(emailAddress = it) } },
                label = { Text(stringResource(R.string.account_email)) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.account_imap_settings))

            OutlinedTextField(
                value = formState.imapServer,
                onValueChange = { update { copy(imapServer = it) } },
                label = { Text(stringResource(R.string.account_imap_server)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.imapPort,
                    onValueChange = { update { copy(imapPort = it) } },
                    label = { Text(stringResource(R.string.account_imap_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                SecurityDropdown(
                    label = stringResource(R.string.account_imap_security),
                    value = formState.imapSecurity,
                    onValueSelected = { update { copy(imapSecurity = it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(stringResource(R.string.account_smtp_settings))

            OutlinedTextField(
                value = formState.smtpServer,
                onValueChange = { update { copy(smtpServer = it) } },
                label = { Text(stringResource(R.string.account_smtp_server)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = formState.smtpPort,
                    onValueChange = { update { copy(smtpPort = it) } },
                    label = { Text(stringResource(R.string.account_smtp_port)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                SecurityDropdown(
                    label = stringResource(R.string.account_smtp_security),
                    value = formState.smtpSecurity,
                    onValueSelected = { update { copy(smtpSecurity = it) } },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = formState.username,
                onValueChange = { update { copy(username = it) } },
                label = { Text(stringResource(R.string.account_username)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = formState.password,
                onValueChange = { update { copy(password = it) } },
                label = { Text(stringResource(R.string.account_password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                placeholder = {
                    if (!isNewAccount) {
                        Text(stringResource(R.string.account_password_hint))
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
                                imapTestState.isLoading -> stringResource(R.string.account_testing_imap)
                                else -> stringResource(R.string.account_test_imap)
                            }
                        )
                    }
                    Button(
                        onClick = onTestSmtp,
                        enabled = canTestConnection && !smtpTestState.isLoading
                    ) {
                        Text(
                            when {
                                smtpTestState.isLoading -> stringResource(R.string.account_testing_smtp)
                                else -> stringResource(R.string.account_test_smtp)
                            }
                        )
                    }
                }

                if (imapTestState.success) {
                    Text(
                        text = stringResource(R.string.account_test_imap_success),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                imapTestState.errorResId?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                imapTestState.error?.takeUnless { it.isNullOrBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (smtpTestState.success) {
                    Text(
                        text = stringResource(R.string.account_test_smtp_success),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                smtpTestState.errorResId?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                smtpTestState.error?.takeUnless { it.isNullOrBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Text(stringResource(R.string.account_signature_section))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.account_enable_signature))
                Switch(
                    checked = formState.signatureEnabled,
                    onCheckedChange = { isEnabled -> update { copy(signatureEnabled = isEnabled) } }
                )
            }

            OutlinedTextField(
                value = formState.signature,
                onValueChange = { update { copy(signature = it) } },
                label = { Text(stringResource(R.string.account_signature_text)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                minLines = 3
            )

            Button(
                onClick = {
                    onSave(formState)
                    onClose()
                },
                enabled = isSaveEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.account_save))
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
    val selectedText = when (value) {
        MailSecurity.SSL_TLS -> stringResource(R.string.security_ssl_tls)
        MailSecurity.STARTTLS -> stringResource(R.string.security_starttls)
        MailSecurity.NONE -> stringResource(R.string.security_none)
    }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        Icons.Outlined.ArrowDropDown,
                        contentDescription = stringResource(R.string.content_expand_dropdown, label)
                    )
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
                    text = {
                        Text(
                            when (option) {
                                MailSecurity.SSL_TLS -> stringResource(R.string.security_ssl_tls)
                                MailSecurity.STARTTLS -> stringResource(R.string.security_starttls)
                                MailSecurity.NONE -> stringResource(R.string.security_none)
                            }
                        )
                    },
                    onClick = {
                        expanded = false
                        onValueSelected(option)
                    }
                )
            }
        }
    }
}

