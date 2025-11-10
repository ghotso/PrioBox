package com.priobox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.priobox.data.model.EmailFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailFolderDao {

    @Query(
        """
        SELECT * FROM email_folders
        WHERE accountId = :accountId
        ORDER BY displayName COLLATE NOCASE
        """
    )
    fun observeFolders(accountId: Long): Flow<List<EmailFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(folders: List<EmailFolder>)

    @Query("DELETE FROM email_folders WHERE accountId = :accountId")
    suspend fun deleteForAccount(accountId: Long)

    @Transaction
    suspend fun replaceFolders(accountId: Long, items: List<EmailFolder>) {
        deleteForAccount(accountId)
        if (items.isNotEmpty()) {
            upsertAll(items)
        }
    }
}
