package com.vipmail.data.network

import com.vipmail.data.model.EmailAccount
import com.vipmail.data.model.EmailMessage
import java.text.Normalizer
import java.util.Date
import javax.inject.Inject
import javax.mail.Address
import javax.mail.Message
import javax.mail.internet.InternetAddress

class MailParser @Inject constructor() {

    fun parseMessage(account: EmailAccount, message: Message, uidOverride: String? = null): EmailMessage? {
        val uid = uidOverride ?: message.messageNumber.toString()
        val fromAddress = message.from?.firstOrNull()?.safeAddress() ?: return null
        val subject = message.subject?.clean() ?: "(No subject)"

        val body = extractBody(message)
        val preview = body.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.take(140)
            ?.clean()
            ?: ""

        return EmailMessage(
            accountId = account.id,
            uid = uid,
            sender = fromAddress,
            subject = subject,
            preview = preview,
            body = body,
            timestamp = (message.receivedDate ?: Date()).time
        )
    }

    private fun extractBody(message: Message): String = when (val content = message.content) {
        is String -> content
        is javax.mail.Multipart -> {
            (0 until content.count)
                .asSequence()
                .mapNotNull { index ->
                    val part = content.getBodyPart(index)
                    when {
                        part.isMimeType("text/plain") -> part.content as? String
                        part.isMimeType("text/html") -> (part.content as? String)?.stripHtml()
                        else -> null
                    }
                }
                .firstOrNull()
                ?: ""
        }
        else -> content.toString()
    }

    private fun String.stripHtml(): String =
        this.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .clean()

    private fun String.clean(): String = Normalizer.normalize(this, Normalizer.Form.NFKC).trim()

    private fun Address.safeAddress(): String? = when (this) {
        is InternetAddress -> address
        else -> toString()
    }
}

