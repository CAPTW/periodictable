package com.chemtable.interactive.app.di

import android.content.Context
import androidx.room.Room
import com.chemtable.interactive.core.database.ChemTableDatabase
import com.chemtable.interactive.core.database.dao.CalcHistoryDao
import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.database.dao.GlossaryDao
import com.chemtable.interactive.core.database.dao.NoteDao
import com.chemtable.interactive.data.prepopulate.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): ChemTableDatabase = Room.databaseBuilder(
        context,
        ChemTableDatabase::class.java,
        "chemtable.db"
    ).build()

    @Provides
    fun provideElementDao(database: ChemTableDatabase): ElementDao = database.elementDao()

    @Provides
    fun provideNoteDao(database: ChemTableDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideGlossaryDao(database: ChemTableDatabase): GlossaryDao = database.glossaryDao()

    @Provides
    fun provideCalcHistoryDao(database: ChemTableDatabase): CalcHistoryDao = database.calcHistoryDao()

    @Provides
    @Singleton
    fun provideDatabaseSeeder(
        @ApplicationContext context: Context,
        elementDao: ElementDao,
        glossaryDao: GlossaryDao
    ): DatabaseSeeder = DatabaseSeeder(context, elementDao, glossaryDao)
}
