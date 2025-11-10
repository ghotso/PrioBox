package com.priobox.ui.compose

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.priobox.R
import com.priobox.data.model.EmailAccount
import com.priobox.data.model.EmailAttachment
import jp.wasabeef.richeditor.RichEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    state: ComposeViewModel.State,
    onClose: () -> Unit,
    onSelectAccount: (EmailAccount) -> Unit,
    onUpdateTo: (String) -> Unit,
    onUpdateSubject: (String) -> Unit,
    onUpdateBodyHtml: (String) -> Unit,
    onAddAttachment: (EmailAttachment) -> Unit,
    onRemoveAttachment: (EmailAttachment) -> Unit,
    onSend: () -> Unit
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val accountMenuExpanded = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var richEditor by remember { mutableStateOf<RichEditor?>(null) }
    var internalHtml by remember { mutableStateOf(state.bodyHtml) }
    var fontMenuExpanded by remember { mutableStateOf(false) }

    val currentState by rememberUpdatedState(state)

    BackHandler { onClose() }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    val attachmentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleAttachmentPick(context, coroutineScope, snackbarHostState, it, inline = false, onAddAttachment = onAddAttachment, richEditor = null) }
    }

    val inlineImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleAttachmentPick(context, coroutineScope, snackbarHostState, it, inline = true, onAddAttachment = onAddAttachment, richEditor = { richEditor }) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                ),
                enabled = !state.isSending
            )

            OutlinedTextField(
                value = state.subject,
                onValueChange = onUpdateSubject,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.compose_subject)) },
                enabled = !state.isSending
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.compose_body))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { richEditor?.setBold() }, enabled = !state.isSending) {
                        Icon(Icons.Outlined.FormatBold, contentDescription = stringResource(R.string.compose_toolbar_bold))
                    }
                    IconButton(onClick = { richEditor?.setItalic() }, enabled = !state.isSending) {
                        Icon(Icons.Outlined.FormatItalic, contentDescription = stringResource(R.string.compose_toolbar_italic))
                    }
                    IconButton(onClick = { richEditor?.setUnderline() }, enabled = !state.isSending) {
                        Icon(Icons.Outlined.FormatUnderlined, contentDescription = stringResource(R.string.compose_toolbar_underline))
                    }
                    Box {
                        IconButton(onClick = { fontMenuExpanded = true }, enabled = !state.isSending) {
                            Icon(Icons.Outlined.FormatSize, contentDescription = stringResource(R.string.compose_toolbar_font_size))
                        }
                        DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                            FontSizeOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes)) },
                                    onClick = {
                                        fontMenuExpanded = false
                                        richEditor?.setFontSize(option.value)
                                    }
                                )
                            }
                        }
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        RichEditor(ctx).apply {
                            setPadding(16, 16, 16, 16)
                            setEditorFontSize(16)
                            setPlaceholder(ctx.getString(R.string.compose_body))
                            setOnTextChangeListener { html ->
                                val next = html.orEmpty()
                                if (internalHtml != next) {
                                    internalHtml = next
                                    onUpdateBodyHtml(next)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp)
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    update = { editor ->
                        richEditor = editor
                        val current = editor.html ?: ""
                        if (internalHtml != currentState.bodyHtml) {
                            internalHtml = currentState.bodyHtml
                            if (current != currentState.bodyHtml) {
                                editor.html = currentState.bodyHtml
                            }
                        }
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { attachmentPicker.launch(arrayOf("*/*")) },
                    enabled = !state.isSending
                ) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.compose_add_attachment))
                }
                OutlinedButton(
                    onClick = { inlineImagePicker.launch(arrayOf("image/*")) },
                    enabled = !state.isSending
                ) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(R.string.compose_insert_image))
                }
            }

            if (state.attachments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.compose_attachments), style = MaterialTheme.typography.titleMedium)
                    state.attachments.forEach { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            canRemove = !attachment.inline,
                            enabled = !state.isSending,
                            onRemove = onRemoveAttachment
                        )
                    }
                }
            }

            Divider()

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

private enum class FontSizeOption(@StringRes val labelRes: Int, val value: Int) {
    Small(R.string.compose_font_size_small, 3),
    Normal(R.string.compose_font_size_normal, 4),
    Large(R.string.compose_font_size_large, 5)
}

@Composable
private fun AttachmentRow(
    attachment: EmailAttachment,
    canRemove: Boolean,
    enabled: Boolean,
    onRemove: (EmailAttachment) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (attachment.inline) Icons.Outlined.Image else Icons.Outlined.AttachFile,
            contentDescription = null
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(attachment.displayName, style = MaterialTheme.typography.bodyMedium)
            Text(attachment.mimeType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (attachment.inline) {
                AssistChip(onClick = {}, enabled = false, label = { Text(stringResource(R.string.compose_inline_indicator)) })
            }
        }
        if (canRemove) {
            IconButton(onClick = { onRemove(attachment) }, enabled = enabled) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.compose_remove_attachment))
            }
        }
    }
}

private fun handleAttachmentPick(
    context: Context,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    uri: Uri,
    inline: Boolean,
    onAddAttachment: (EmailAttachment) -> Unit,
    richEditor: (() -> RichEditor?)?
) {
    coroutineScope.launch {
        val attachment = resolveAttachment(context, uri, inline)
        if (attachment == null) {
            snackbarHostState.showSnackbar(context.getString(R.string.compose_attachment_error))
            return@launch
        }
        onAddAttachment(attachment)
        if (inline) {
            val placeholder = attachment.placeholder
            if (placeholder != null) {
                richEditor?.invoke()?.let { editor ->
                    editor.insertImage(placeholder, attachment.displayName)
                    editor.focusEditor()
                }
            }
        }
    }
}

private suspend fun resolveAttachment(
    context: Context,
    uri: Uri,
    inline: Boolean
): EmailAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri) ?: if (inline) "image/*" else "application/octet-stream"
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index != -1 && cursor.moveToFirst()) {
            cursor.getString(index)
        } else {
            null
        }
    } ?: uri.lastPathSegment ?: context.getString(R.string.compose_default_attachment_name)

    try {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } catch (_: SecurityException) {
        // Ignore if we already hold permission
    }

    return@withContext if (inline) {
        val bytes = try {
            resolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: IOException) {
            null
        }
        if (bytes == null) {
            null
        } else {
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val placeholder = "data:$mimeType;base64,$base64"
            EmailAttachment(
                uri = null,
                displayName = name,
                mimeType = mimeType,
                inline = true,
                contentId = "img-${UUID.randomUUID()}",
                data = bytes,
                placeholder = placeholder
            )
        }
    } else {
        EmailAttachment(
            uri = uri,
            displayName = name,
            mimeType = mimeType,
            inline = false
        )
    }
}

