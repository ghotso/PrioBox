package com.priobox.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.priobox.data.model.VipSender
import kotlinx.coroutines.flow.Flow

@Dao
interface VipSenderDao {

    @Query(
        """
        SELECT * FROM vip_senders
        WHERE accountId = :accountId
        ORDER BY emailAddress ASC
        """
    )
    fun observeVipSenders(accountId: Long): Flow<List<VipSender>>

    @Query(
        """
        SELECT * FROM vip_senders
        WHERE accountId = :accountId AND emailAddress = :email
        LIMIT 1
        """
    )
    suspend fun getVipSender(accountId: Long, email: String): VipSender?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vipSender: VipSender): Long

    @Delete
    suspend fun delete(vipSender: VipSender)
}

