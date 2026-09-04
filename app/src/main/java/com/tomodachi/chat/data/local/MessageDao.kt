package com.tomodachi.chat.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM cached_messages ORDER BY createdAtMillis ASC")
    fun observeMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("DELETE FROM cached_messages WHERE id = :messageId")
    suspend fun deleteById(messageId: String)

    @Query("DELETE FROM cached_messages")
    suspend fun clearAll()

    /**
     * يحافظ على آخر 500 رسالة فقط محلياً، ويحذف الأقدم.
     */
    @Query(
        """
        DELETE FROM cached_messages WHERE id NOT IN (
            SELECT id FROM cached_messages ORDER BY createdAtMillis DESC LIMIT :limit
        )
        """
    )
    suspend fun trimToLimit(limit: Int = 500)
}
