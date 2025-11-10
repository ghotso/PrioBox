package com.vipmail.data.model

enum class MailSecurity(val displayName: String) {
    SSL_TLS("SSL/TLS"),
    STARTTLS("STARTTLS"),
    NONE("None");

    companion object {
        fun fromDisplayName(name: String): MailSecurity =
            values().firstOrNull { it.displayName == name } ?: NONE
    }
}

