package com.priobox.ui.vip

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.VipSender
import com.priobox.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipListScreen(
    state: VipViewModel.State,
    onAction: (VipViewModel.Action) -> Unit,
    onSelectAccount: (EmailAccount) -> Unit,
    onNewVipChange: (String) -> Unit,
    onAddVip: () -> Unit,
    onRemoveVip: (VipSender) -> Unit
) {
    BackHandler { onAction(VipViewModel.Action.NavigateBack) }

    val accountMenuExpanded = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vip_title)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(VipViewModel.Action.NavigateBack) }) {
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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.selectedAccount?.let { selected ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.vip_account_label),
                        style = MaterialTheme.typography.labelSmall
                    )
                    OutlinedButton(onClick = { accountMenuExpanded.value = true }) {
                        Text("${selected.displayName} (${selected.emailAddress})")
                    }
                    DropdownMenu(
                        expanded = accountMenuExpanded.value,
                        onDismissRequest = { accountMenuExpanded.value = false }
                    ) {
                        state.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.displayName} (${account.emailAddress})") },
                                onClick = {
                                    accountMenuExpanded.value = false
                                    onSelectAccount(account)
                                }
                            )
                        }
                    }
                }
            } ?: Text(stringResource(R.string.vip_no_accounts))

            OutlinedTextField(
                value = state.newVipEmail,
                onValueChange = onNewVipChange,
                label = { Text(stringResource(R.string.vip_email_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) }
            )

            TextButton(
                onClick = onAddVip,
                enabled = state.selectedAccount != null
            ) {
                Text(stringResource(R.string.vip_add_button))
            }

            state.vipSenders.forEach { vip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(vip.emailAddress)
                    IconButton(onClick = { onRemoveVip(vip) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.vip_remove)
                        )
                    }
                }
            }
        }
    }
}

