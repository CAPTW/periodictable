package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.database.dao.GlossaryDao
import com.chemtable.interactive.core.database.dao.IsotopeDao

class DatabaseSeeder(
    private val context: Context,
    private val elementDao: ElementDao,
    private val isotopeDao: IsotopeDao,
    private val glossaryDao: GlossaryDao
) {
    suspend fun seedIfNeeded() {
        val elements = ElementDataLoader(context).load()
        if (elements.isNotEmpty()) {
            elementDao.upsertElements(elements)
        }

        val isotopes = IsotopeDataLoader(context).load()
        if (isotopes.isNotEmpty()) {
            isotopeDao.upsertIsotopes(isotopes)
        }

        val terms = GlossaryDataLoader(context).load()
        if (terms.isNotEmpty()) {
            glossaryDao.upsertTerms(terms)
        }
    }
}
