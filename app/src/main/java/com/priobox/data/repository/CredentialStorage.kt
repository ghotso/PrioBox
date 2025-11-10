package com.priobox.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialStorage @Inject constructor(
    @ApplicationContext context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getPassword(accountId: Long): String? =
        preferences.getString(passwordKey(accountId), null)

    fun setPassword(accountId: Long, password: String) {
        preferences.edit().putString(passwordKey(accountId), password).apply()
    }

    fun clearPassword(accountId: Long) {
        preferences.edit().remove(passwordKey(accountId)).apply()
    }

    private fun passwordKey(accountId: Long): String = "account_password_$accountId"

    private companion object {
        const val PREFERENCES_NAME = "secure_credentials"
    }
}

