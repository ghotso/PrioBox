package com.priobox.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.priobox.data.model.EmailMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface EmailMessageDao {

    @Query(
        """
        SELECT * FROM email_messages
        WHERE accountId = :accountId AND folder = :folder
        ORDER BY timestamp DESC
        """
    )
    fun observeMessages(accountId: Long, folder: String): Flow<List<EmailMessage>>

    @Query(
        """
        SELECT * FROM email_messages
        WHERE accountId = :accountId AND isVip = 1
        ORDER BY timestamp DESC
        """
    )
    fun observeVipMessages(accountId: Long): Flow<List<EmailMessage>>

    @Query(
        """
        SELECT * FROM email_messages
        WHERE accountId = :accountId AND isVip = 1
        ORDER BY timestamp DESC
        """
    )
    suspend fun getVipMessages(accountId: Long): List<EmailMessage>

    @Query("SELECT * FROM email_messages WHERE id = :id")
    fun observeMessage(id: Long): Flow<EmailMessage?>

    @Query("SELECT * FROM email_messages WHERE id = :id")
    suspend fun getMessage(id: Long): EmailMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<EmailMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: EmailMessage): Long

    @Update
    suspend fun update(message: EmailMessage)

    @Query("DELETE FROM email_messages WHERE accountId = :accountId AND folder = :folder AND uid NOT IN (:uids)")
    suspend fun deleteNotIn(accountId: Long, folder: String, uids: List<String>)

    @Query("DELETE FROM email_messages WHERE accountId = :accountId AND folder = :folder")
    suspend fun deleteForFolder(accountId: Long, folder: String)

    @Transaction
    suspend fun replaceMessages(accountId: Long, folder: String, items: List<EmailMessage>) {
        val uids = items.map { it.uid }
        if (uids.isNotEmpty()) {
            deleteNotIn(accountId, folder, uids)
        } else {
            deleteForFolder(accountId, folder)
        }
        upsertAll(items)
    }

    @Query(
        """
        UPDATE email_messages
        SET isVip = :isVip
        WHERE accountId = :accountId AND LOWER(sender) = LOWER(:email)
        """
    )
    suspend fun updateVipStatus(accountId: Long, email: String, isVip: Boolean)

    @Query("UPDATE email_messages SET isRead = :isRead WHERE id = :id")
    suspend fun updateReadState(id: Long, isRead: Boolean)
}

