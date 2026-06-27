package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.database.dao.GlossaryDao
import com.chemtable.interactive.core.database.dao.IsotopeDao
import com.chemtable.interactive.core.util.StartupTrace

class DatabaseSeeder(
    private val context: Context,
    private val elementDao: ElementDao,
    private val isotopeDao: IsotopeDao,
    private val glossaryDao: GlossaryDao
) {
    suspend fun seedIfNeeded() {
        val elementCount = StartupTrace.measureSuspend("DatabaseSeeder.count.elements") {
            elementDao.countElements()
        }
        if (elementCount > 0) {
            StartupTrace.mark("DatabaseSeeder.skip.elements count=$elementCount")
        } else {
            seedElements()
        }

        val isotopeCount = StartupTrace.measureSuspend("DatabaseSeeder.count.isotopes") {
            isotopeDao.countIsotopes()
        }
        if (isotopeCount > 0) {
            StartupTrace.mark("DatabaseSeeder.skip.isotopes count=$isotopeCount")
        } else {
            seedIsotopes()
        }

        val termCount = StartupTrace.measureSuspend("DatabaseSeeder.count.glossary") {
            glossaryDao.countTerms()
        }
        if (termCount > 0) {
            StartupTrace.mark("DatabaseSeeder.skip.glossary count=$termCount")
        } else {
            seedGlossary()
        }
    }

    private suspend fun seedElements() {
        val elements = StartupTrace.measure("DatabaseSeeder.load.elements") {
            ElementDataLoader(context).load()
        }
        StartupTrace.mark("DatabaseSeeder.elements loaded count=${elements.size}")
        if (elements.isNotEmpty()) {
            StartupTrace.measureSuspend("DatabaseSeeder.upsert.elements") {
                elementDao.upsertElements(elements)
            }
        }
    }

    private suspend fun seedIsotopes() {
        val isotopes = StartupTrace.measure("DatabaseSeeder.load.isotopes") {
            IsotopeDataLoader(context).load()
        }
        StartupTrace.mark("DatabaseSeeder.isotopes loaded count=${isotopes.size}")
        if (isotopes.isNotEmpty()) {
            StartupTrace.measureSuspend("DatabaseSeeder.upsert.isotopes") {
                isotopeDao.upsertIsotopes(isotopes)
            }
        }
    }

    private suspend fun seedGlossary() {
        val terms = StartupTrace.measure("DatabaseSeeder.load.glossary") {
            GlossaryDataLoader(context).load()
        }
        StartupTrace.mark("DatabaseSeeder.glossary loaded count=${terms.size}")
        if (terms.isNotEmpty()) {
            StartupTrace.measureSuspend("DatabaseSeeder.upsert.glossary") {
                glossaryDao.upsertTerms(terms)
            }
        }
    }
}
