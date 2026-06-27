package com.chemtable.interactive.core.database

import androidx.room.migration.Migration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SQL-level safety tests for the Room migration chain (v2 -> v6).
 *
 * These run on the JVM with no device/emulator. They guarantee that the schema upgrade path is
 * contiguous, purely additive (never drops or deletes existing element / glossary / note data),
 * and creates the schema objects the app's entities require. This complements
 * [GameStatsMigrationSqlTest] (which covers the v4->v5 and v5->v6 game-stats SQL specifically).
 */
class MigrationSafetyTest {

    private fun sqlOf(migration: Migration): List<String> = when (migration.startVersion to migration.endVersion) {
        2 to 3 -> DbMigrations.MIGRATION_2_3_SQL
        3 to 4 -> DbMigrations.MIGRATION_3_4_SQL
        4 to 5 -> DbMigrations.MIGRATION_4_5_SQL
        5 to 6 -> DbMigrations.MIGRATION_5_6_SQL
        else -> error("No SQL exposed for migration ${migration.startVersion}->${migration.endVersion}")
    }

    private val allSql: String =
        DbMigrations.ALL.flatMap { sqlOf(it) }.joinToString("\n").uppercase()

    @Test
    fun migrationChain_reachesVersion6Contiguously() {
        val ordered = DbMigrations.ALL.sortedBy { it.startVersion }
        assertEquals("Expected 4 migrations (2->3->4->5->6)", 4, ordered.size)
        assertEquals("Chain must start at version 2", 2, ordered.first().startVersion)
        assertEquals("Chain must reach version 6", 6, ordered.last().endVersion)
        ordered.zipWithNext().forEach { (a, b) ->
            assertEquals(
                "Gap/overlap in migration chain between ${a.startVersion}->${a.endVersion} " +
                    "and ${b.startVersion}->${b.endVersion}",
                a.endVersion,
                b.startVersion,
            )
        }
    }

    @Test
    fun migrations_areNonDestructive_noDropsOrDeletes() {
        assertFalse("A migration drops a table (would lose data)", allSql.contains("DROP TABLE"))
        assertFalse("A migration drops a column (would lose data)", allSql.contains("DROP COLUMN"))
        assertFalse("A migration deletes rows (would lose data)", allSql.contains("DELETE FROM"))
        assertFalse("A migration truncates data", allSql.contains("TRUNCATE"))
    }

    @Test
    fun migration2To3_addsElementColumnsAndCreatesIsotopes() {
        val sql = DbMigrations.MIGRATION_2_3_SQL.joinToString("\n")
        assertEquals(2, DbMigrations.MIGRATION_2_3.startVersion)
        assertEquals(3, DbMigrations.MIGRATION_2_3.endVersion)
        listOf(
            "ALTER TABLE elements ADD COLUMN latin_name TEXT",
            "ALTER TABLE elements ADD COLUMN discovery_year INTEGER",
            "CREATE TABLE IF NOT EXISTS isotopes",
            "index_isotopes_atomic_number",
            "index_isotopes_symbol",
        ).forEach { assertTrue("Missing 2->3 fragment: $it", sql.contains(it)) }
    }

    @Test
    fun migration3To4_isAllAdditiveElementColumns() {
        assertEquals(3, DbMigrations.MIGRATION_3_4.startVersion)
        assertEquals(4, DbMigrations.MIGRATION_3_4.endVersion)
        assertTrue("Expected many element-property columns", DbMigrations.MIGRATION_3_4_SQL.size >= 50)
        DbMigrations.MIGRATION_3_4_SQL.forEach { statement ->
            assertTrue(
                "v3->v4 statement must be an additive ALTER on elements: $statement",
                statement.startsWith("ALTER TABLE elements ADD COLUMN "),
            )
        }
        listOf("proton_count", "crystal_structure", "abundance_crust", "data_confidence")
            .forEach { col ->
                assertTrue(
                    "Missing 3->4 column: $col",
                    DbMigrations.MIGRATION_3_4_SQL.any { it.contains(col) },
                )
            }
    }

    @Test
    fun migration4To5_createsBothGameTables() {
        val sql = DbMigrations.MIGRATION_4_5_SQL.joinToString("\n")
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS game_molecule_discoveries"))
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS game_sessions"))
    }
}
