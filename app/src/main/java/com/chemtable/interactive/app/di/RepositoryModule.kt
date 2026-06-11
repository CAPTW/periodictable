package com.chemtable.interactive.app.di

import com.chemtable.interactive.data.repository.ElementRepositoryImpl
import com.chemtable.interactive.data.repository.GlossaryRepositoryImpl
import com.chemtable.interactive.data.repository.IsotopeRepositoryImpl
import com.chemtable.interactive.data.repository.CalcHistoryRepositoryImpl
import com.chemtable.interactive.data.repository.NoteRepositoryImpl
import com.chemtable.interactive.domain.repository.CalcHistoryRepository
import com.chemtable.interactive.domain.repository.ElementRepository
import com.chemtable.interactive.domain.repository.GlossaryRepository
import com.chemtable.interactive.domain.repository.IsotopeRepository
import com.chemtable.interactive.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindElementRepository(impl: ElementRepositoryImpl): ElementRepository

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    abstract fun bindGlossaryRepository(impl: GlossaryRepositoryImpl): GlossaryRepository

    @Binds
    abstract fun bindIsotopeRepository(impl: IsotopeRepositoryImpl): IsotopeRepository

    @Binds
    abstract fun bindCalcHistoryRepository(impl: CalcHistoryRepositoryImpl): CalcHistoryRepository
}
