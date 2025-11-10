package com.vipmail.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vipmail.data.model.EmailAccount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsViewModel.State,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (EmailAccount) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccount) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add Account")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Text(
                text = "Accounts",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.accounts) { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(account.displayName, fontWeight = FontWeight.SemiBold)
                            Text(account.emailAddress)
                            Text(
                                text = "IMAP: ${account.imapServer}:${account.imapPort}",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = "SMTP: ${account.smtpServer}:${account.smtpPort}",
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            TextButton(
                                onClick = { onEditAccount(account) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text("Edit")
                            }
                        }
                    }
                }
            }
        }
    }
}

