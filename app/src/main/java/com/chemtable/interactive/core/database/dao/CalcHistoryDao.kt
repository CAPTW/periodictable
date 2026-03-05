package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.CalcHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CalcHistoryDao {
    @Query("SELECT * FROM calc_history ORDER BY created_at DESC LIMIT :limit")
    fun getRecent(limit: Int = 20): Flow<List<CalcHistoryEntity>>

    @Query("SELECT COUNT(*) FROM calc_history")
    suspend fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CalcHistoryEntity)

    @Query("DELETE FROM calc_history WHERE id IN (SELECT id FROM calc_history ORDER BY created_at ASC LIMIT :countToRemove)")
    suspend fun trim(countToRemove: Int)
}
