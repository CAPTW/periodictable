package com.chemtable.interactive.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigration6To7AndroidTest {

    @Test
    fun migrationPreservesExistingSessionsAndDiscoveriesAndSupportsFiveAndSix() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "p1_migration_${System.nanoTime()}.db"
        context.deleteDatabase(databaseName)

        try {
            createVersion6Database(context, databaseName)

            val helper = openVersion7Database(context, databaseName)
            val db = helper.writableDatabase

            assertEquals(7, queryInt(db, "PRAGMA user_version"))
            assertEquals(2, queryInt(db, "SELECT COUNT(*) FROM game_sessions"))
            assertEquals(1, queryInt(db, "SELECT COUNT(*) FROM game_molecule_discoveries"))
            assertEquals(2, queryInt(db, "SELECT COUNT(*) FROM game_sessions WHERE boardSize = 4"))
            assertEquals(900, queryInt(db, "SELECT MAX(score) FROM game_sessions WHERE boardSize = 4"))

            insertSession(db, score = 1_500, boardSize = 5)
            insertSession(db, score = 2_400, boardSize = 6)

            assertEquals(1_500, queryInt(db, "SELECT MAX(score) FROM game_sessions WHERE boardSize = 5"))
            assertEquals(2_400, queryInt(db, "SELECT MAX(score) FROM game_sessions WHERE boardSize = 6"))
            assertTrue(queryInt(db, "SELECT COUNT(*) FROM game_sessions") == 4)
            helper.close()
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersion6Database(context: Context, name: String) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE game_sessions (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                score INTEGER NOT NULL,
                                success INTEGER NOT NULL,
                                difficulty TEXT NOT NULL,
                                missionFormula TEXT,
                                missionTargetCount INTEGER,
                                playedAt INTEGER NOT NULL,
                                moleculesMade TEXT NOT NULL,
                                mode TEXT NOT NULL DEFAULT 'MISSION'
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE game_molecule_discoveries (
                                formula TEXT NOT NULL,
                                firstDiscoveredAt INTEGER NOT NULL,
                                lastDiscoveredAt INTEGER NOT NULL,
                                discoveryCount INTEGER NOT NULL,
                                PRIMARY KEY(formula)
                            )
                            """.trimIndent()
                        )
                        insertSession(db, score = 500)
                        insertSession(db, score = 900)
                        db.execSQL(
                            "INSERT INTO game_molecule_discoveries VALUES ('H2O', 100, 200, 2)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase
        helper.close()
    }

    private fun openVersion7Database(context: Context, name: String): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        assertEquals(6, oldVersion)
                        assertEquals(7, newVersion)
                        DbMigrations.MIGRATION_6_7.migrate(db)
                    }
                })
                .build()
        )

    private fun insertSession(db: SupportSQLiteDatabase, score: Int, boardSize: Int? = null) {
        if (boardSize == null) {
            db.execSQL(
                """
                INSERT INTO game_sessions(
                    score, success, difficulty, missionFormula, missionTargetCount,
                    playedAt, moleculesMade, mode
                ) VALUES (?, 1, 'BEGINNER', 'H2O', 2, ?, '[\"H2O\"]', 'MISSION')
                """.trimIndent(),
                arrayOf<Any>(score, score.toLong()),
            )
        } else {
            db.execSQL(
                """
                INSERT INTO game_sessions(
                    score, success, difficulty, missionFormula, missionTargetCount,
                    playedAt, moleculesMade, mode, boardSize
                ) VALUES (?, 1, 'BEGINNER', 'H2O', 2, ?, '[\"H2O\"]', 'MISSION', ?)
                """.trimIndent(),
                arrayOf<Any>(score, score.toLong(), boardSize),
            )
        }
    }

    private fun queryInt(db: SupportSQLiteDatabase, sql: String): Int =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst()) { "Query returned no rows: $sql" }
            cursor.getInt(0)
        }
}
