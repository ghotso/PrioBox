package com.priobox.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.priobox.data.model.EmailAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailAccountDao {

    @Query("SELECT * FROM email_accounts ORDER BY id ASC")
    fun observeAccounts(): Flow<List<EmailAccount>>

    @Query("SELECT * FROM email_accounts WHERE id = :id")
    fun observeAccount(id: Long): Flow<EmailAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: EmailAccount): Long

    @Update
    suspend fun update(account: EmailAccount)

    @Delete
    suspend fun delete(account: EmailAccount)

    @Query("SELECT * FROM email_accounts LIMIT 1")
    suspend fun getFirstAccount(): EmailAccount?
}

