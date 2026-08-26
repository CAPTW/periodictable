package com.chemtable.interactive.core.database

import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import com.chemtable.interactive.core.database.entity.ElementEntity
import com.chemtable.interactive.core.database.entity.IsotopeEntity
import com.chemtable.interactive.core.database.entity.CalcHistoryEntity
import com.chemtable.interactive.core.database.entity.GameMoleculeDiscoveryEntity
import com.chemtable.interactive.core.database.entity.GameSessionEntity
import com.chemtable.interactive.core.database.entity.GlossaryEntity
import com.chemtable.interactive.core.database.entity.NoteEntity
import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.database.dao.GameStatsDao
import com.chemtable.interactive.core.database.dao.IsotopeDao
import com.chemtable.interactive.core.database.dao.NoteDao
import com.chemtable.interactive.core.database.dao.GlossaryDao
import com.chemtable.interactive.core.database.dao.CalcHistoryDao
import com.chemtable.interactive.core.database.converter.DbTypeConverters

@Database(
    entities = [
        ElementEntity::class,
        IsotopeEntity::class,
        NoteEntity::class,
        GlossaryEntity::class,
        CalcHistoryEntity::class,
        GameMoleculeDiscoveryEntity::class,
        GameSessionEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DbTypeConverters::class)
abstract class ChemTableDatabase : RoomDatabase() {
    abstract fun elementDao(): ElementDao
    abstract fun isotopeDao(): IsotopeDao
    abstract fun noteDao(): NoteDao
    abstract fun glossaryDao(): GlossaryDao
    abstract fun calcHistoryDao(): CalcHistoryDao
    abstract fun gameStatsDao(): GameStatsDao
}
