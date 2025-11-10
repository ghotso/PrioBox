package com.priobox.data.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class EmailAttachment(
    val uri: Uri? = null,
    val displayName: String,
    val mimeType: String,
    val inline: Boolean = false,
    val contentId: String? = null,
    val data: ByteArray? = null,
    val placeholder: String? = null
)
