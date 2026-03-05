package com.chemtable.interactive.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chemtable.interactive.core.database.entity.ElementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ElementDao {
    @Query("SELECT COUNT(*) FROM elements")
    suspend fun countElements(): Int

    @Query("SELECT * FROM elements ORDER BY atomic_number")
    fun getAllElements(): Flow<List<ElementEntity>>

    @Query("SELECT * FROM elements WHERE atomic_number = :number LIMIT 1")
    suspend fun getElementById(number: Int): ElementEntity?

    @Query("""
        SELECT * FROM elements 
        WHERE name LIKE '%' || :query || '%' 
           OR name_ko LIKE '%' || :query || '%' 
           OR symbol LIKE '%' || :query || '%'
    """)
    fun searchElements(query: String): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE molar_mass BETWEEN :min AND :max
    """)
    fun filterByMolarMass(min: Double, max: Double): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE atomic_radius BETWEEN :min AND :max
    """)
    fun filterByAtomicRadius(min: Double, max: Double): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE electro_neg BETWEEN :min AND :max
    """)
    fun filterByElectronegativity(min: Double, max: Double): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE thermal_cond BETWEEN :min AND :max
    """)
    fun filterByThermalConductivity(min: Double, max: Double): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE symbol LIKE :symbol || '%'
    """)
    fun searchBySymbol(symbol: String): Flow<List<ElementEntity>>

    @Query("""
        SELECT * FROM elements 
        WHERE atomic_number = :number
    """)
    fun getElementByNumber(number: Int): Flow<ElementEntity?>

    @Query("""
        SELECT * FROM elements 
        WHERE name LIKE '%' || :query || '%' 
           OR name_ko LIKE '%' || :query || '%' 
           OR atomic_number LIKE '%' || :query || '%'
    """)
    fun searchByNameOrNumber(query: String): Flow<List<ElementEntity>>

    @Query("SELECT * FROM elements WHERE atomic_number IN (:atomicNumbers)")
    fun getElementsByNumbers(atomicNumbers: List<Int>): Flow<List<ElementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertElements(elements: List<ElementEntity>)
}
