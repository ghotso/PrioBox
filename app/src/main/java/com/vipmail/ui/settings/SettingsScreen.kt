package com.priobox.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.priobox.R
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.MailSecurity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsViewModel.State,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (EmailAccount) -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.content_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccount) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.settings_add_account)
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
                .padding(innerPadding)
        ) {
            Text(
                text = stringResource(R.string.settings_accounts),
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
                                text = stringResource(
                                    R.string.settings_account_imap_summary,
                                    account.imapServer,
                                    account.imapPort.toString(),
                                    securityDisplayName(account.imapSecurity)
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_account_smtp_summary,
                                    account.smtpServer,
                                    account.smtpPort.toString(),
                                    securityDisplayName(account.smtpSecurity)
                                ),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            TextButton(
                                onClick = { onEditAccount(account) },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.settings_edit))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun securityDisplayName(security: MailSecurity): String =
    when (security) {
        MailSecurity.SSL_TLS -> stringResource(R.string.security_ssl_tls)
        MailSecurity.STARTTLS -> stringResource(R.string.security_starttls)
        MailSecurity.NONE -> stringResource(R.string.security_none)
    }

