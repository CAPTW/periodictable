package com.chemtable.interactive.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStatsMigrationSqlTest {

    @Test
    fun migration4To5_hasExpectedVersionRange() {
        assertEquals(4, DbMigrations.MIGRATION_4_5.startVersion)
        assertEquals(5, DbMigrations.MIGRATION_4_5.endVersion)
    }

    @Test
    fun migration4To5_createsGameStatsTablesAndIndexes() {
        val sql = DbMigrations.MIGRATION_4_5_SQL.joinToString("\n")

        listOf(
            "game_molecule_discoveries",
            "formula TEXT NOT NULL",
            "firstDiscoveredAt INTEGER NOT NULL",
            "lastDiscoveredAt INTEGER NOT NULL",
            "discoveryCount INTEGER NOT NULL",
            "game_sessions",
            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL",
            "score INTEGER NOT NULL",
            "success INTEGER NOT NULL",
            "difficulty TEXT NOT NULL",
            "missionFormula TEXT",
            "missionTargetCount INTEGER",
            "playedAt INTEGER NOT NULL",
            "moleculesMade TEXT NOT NULL",
            "index_game_molecule_discoveries_lastDiscoveredAt",
            "index_game_molecule_discoveries_discoveryCount",
            "index_game_sessions_playedAt",
            "index_game_sessions_score",
            "index_game_sessions_difficulty",
        ).forEach { expected ->
            assertTrue("Missing migration SQL fragment: $expected", sql.contains(expected))
        }
    }

    @Test
    fun migration5To6_hasExpectedVersionRange() {
        assertEquals(5, DbMigrations.MIGRATION_5_6.startVersion)
        assertEquals(6, DbMigrations.MIGRATION_5_6.endVersion)
    }

    @Test
    fun migration5To6_addsSessionModeColumnAndIndex() {
        val sql = DbMigrations.MIGRATION_5_6_SQL.joinToString("\n")

        listOf(
            "ALTER TABLE game_sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'MISSION'",
            "index_game_sessions_mode",
            "game_sessions(mode)",
        ).forEach { expected ->
            assertTrue("Missing migration SQL fragment: $expected", sql.contains(expected))
        }
    }

    @Test
    fun migration6To7_addsBoardSizeWithFourByFourDefaultAndMatchingIndex() {
        assertEquals(6, DbMigrations.MIGRATION_6_7.startVersion)
        assertEquals(7, DbMigrations.MIGRATION_6_7.endVersion)

        val sql = DbMigrations.MIGRATION_6_7_SQL.joinToString("\n")
        listOf(
            "ALTER TABLE game_sessions ADD COLUMN boardSize INTEGER NOT NULL DEFAULT 4",
            "index_game_sessions_boardSize",
            "game_sessions(boardSize)",
        ).forEach { expected ->
            assertTrue("Missing migration SQL fragment: $expected", sql.contains(expected))
        }
        listOf("DROP ", "DELETE ", "TRUNCATE", "UPDATE ").forEach { destructive ->
            assertTrue("Migration must not contain $destructive", !sql.uppercase().contains(destructive))
        }
    }
}
