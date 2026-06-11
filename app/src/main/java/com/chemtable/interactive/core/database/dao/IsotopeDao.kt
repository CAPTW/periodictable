package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.IsotopeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IsotopeDao {
    @Query("SELECT COUNT(*) FROM isotopes")
    suspend fun countIsotopes(): Int

    @Query("SELECT * FROM isotopes ORDER BY atomic_number, mass_number")
    fun getAllIsotopes(): Flow<List<IsotopeEntity>>

    @Query(
        """
        SELECT * FROM isotopes
        WHERE atomic_number = :atomicNumber
        ORDER BY is_stable DESC, mass_number
        """
    )
    fun getIsotopesByAtomicNumber(atomicNumber: Int): Flow<List<IsotopeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIsotopes(isotopes: List<IsotopeEntity>)
}
