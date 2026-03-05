package com.chemtable.interactive.data.prepopulate

import android.content.Context
import com.chemtable.interactive.core.database.dao.ElementDao
import com.chemtable.interactive.core.database.dao.GlossaryDao

class DatabaseSeeder(
    private val context: Context,
    private val elementDao: ElementDao,
    private val glossaryDao: GlossaryDao
) {
    suspend fun seedIfNeeded() {
        if (elementDao.countElements() == 0) {
            val elements = ElementDataLoader(context).load()
            if (elements.isNotEmpty()) {
                elementDao.upsertElements(elements)
            }
        }

        if (glossaryDao.countTerms() == 0) {
            val terms = GlossaryDataLoader(context).load()
            if (terms.isNotEmpty()) {
                glossaryDao.upsertTerms(terms)
            }
        }
    }
}
