package com.priobox.ui.main

import androidx.compose.runtime.Composable
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.priobox.ui.compose.ComposeScreen
import com.priobox.ui.compose.ComposeViewModel
import com.priobox.ui.inbox.InboxScreen
import com.priobox.ui.inbox.InboxViewModel
import com.priobox.ui.inbox.MessageDetailScreen
import com.priobox.ui.inbox.MessageDetailViewModel
import com.priobox.ui.settings.AccountEditScreen
import com.priobox.ui.settings.SettingsScreen
import com.priobox.ui.settings.SettingsViewModel
import com.priobox.ui.vip.VipListScreen
import com.priobox.ui.vip.VipViewModel

object Destinations {
    const val INBOX = "inbox"
    const val COMPOSE = "compose"
    const val SETTINGS = "settings"
    const val ACCOUNT_EDIT = "account_edit"
    const val VIP = "vip"
    const val MESSAGE_DETAIL = "message_detail"
}

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController()
) {
    val activity = LocalContext.current as ComponentActivity

    NavHost(
        navController = navController,
        startDestination = Destinations.INBOX
    ) {
        composable(Destinations.INBOX) {
            val viewModel: InboxViewModel = hiltViewModel()
            val settingsViewModel: SettingsViewModel = hiltViewModel(activity)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            InboxScreen(
                state = state,
                onAction = { action ->
                    viewModel.onAction(action)
                    when (action) {
                        InboxViewModel.Action.OpenCompose -> navController.navigate(Destinations.COMPOSE)
                        InboxViewModel.Action.OpenSettings -> navController.navigate(Destinations.SETTINGS)
                        is InboxViewModel.Action.OpenVip -> navController.navigate(Destinations.VIP)
                        is InboxViewModel.Action.OpenMessage ->
                            navController.navigate("${Destinations.MESSAGE_DETAIL}/${action.messageId}")
                        InboxViewModel.Action.CreateFirstAccount -> {
                            settingsViewModel.startCreateAccount()
                            navController.navigate(Destinations.ACCOUNT_EDIT)
                        }
                        InboxViewModel.Action.NavigateBack -> navController.navigateUp()
                        else -> Unit
                    }
                }
            )
        }

        composable(Destinations.COMPOSE) {
            val viewModel: ComposeViewModel = hiltViewModel()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            ComposeScreen(
                state = state,
                onClose = { navController.navigateUp() },
                onSelectAccount = viewModel::selectAccount,
                onUpdateTo = viewModel::updateTo,
                onUpdateSubject = viewModel::updateSubject,
                onUpdateBodyHtml = viewModel::updateBodyHtml,
                onAddAttachment = viewModel::addAttachment,
                onRemoveAttachment = viewModel::removeAttachment,
                onSend = {
                    viewModel.send { action ->
                        when (action) {
                            ComposeViewModel.Action.Close -> navController.navigateUp()
                            is ComposeViewModel.Action.EmailSent -> navController.navigateUp()
                        }
                    }
                }
            )
        }

        composable(Destinations.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel(activity)
            val state = viewModel.state.collectAsStateWithLifecycle().value
            SettingsScreen(
                state = state,
                onBack = { navController.navigateUp() },
                onAddAccount = {
                    viewModel.startCreateAccount()
                    navController.navigate(Destinations.ACCOUNT_EDIT)
                },
                onEditAccount = { account ->
                    viewModel.startEditAccount(account)
                    navController.navigate(Destinations.ACCOUNT_EDIT)
                }
            )
        }

        composable(Destinations.ACCOUNT_EDIT) {
            val viewModel: SettingsViewModel = hiltViewModel(activity)
            val settingsState = viewModel.state.collectAsStateWithLifecycle().value
            AccountEditScreen(
                state = settingsState.accountEditorState,
                imapTestState = settingsState.imapTestState,
                smtpTestState = settingsState.smtpTestState,
                onClose = { navController.navigateUp() },
                onUpdate = viewModel::onEditorChange,
                onSave = viewModel::onAccountSaved,
                onTestImap = viewModel::testImapConnection,
                onTestSmtp = viewModel::testSmtpConnection
            )
        }

        composable(Destinations.VIP) {
            val viewModel: VipViewModel = hiltViewModel()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            VipListScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        VipViewModel.Action.NavigateBack -> navController.navigateUp()
                    }
                },
                onSelectAccount = viewModel::selectAccount,
                onNewVipChange = viewModel::updateNewVipEmail,
                onAddVip = viewModel::addVip,
                onRemoveVip = viewModel::removeVip
            )
        }

        composable(
            route = "${Destinations.MESSAGE_DETAIL}/{messageId}",
            arguments = listOf(navArgument("messageId") { type = NavType.LongType })
        ) { _ ->
            val viewModel: MessageDetailViewModel = hiltViewModel()
            val state = viewModel.state.collectAsStateWithLifecycle().value
            MessageDetailScreen(
                state = state,
                onBack = { navController.navigateUp() }
            )
        }
    }
}

