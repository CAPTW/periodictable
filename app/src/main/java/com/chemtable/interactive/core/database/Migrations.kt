package com.chemtable.interactive.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DbMigrations {
    internal val MIGRATION_4_5_SQL: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS game_molecule_discoveries (
            formula TEXT NOT NULL,
            firstDiscoveredAt INTEGER NOT NULL,
            lastDiscoveredAt INTEGER NOT NULL,
            discoveryCount INTEGER NOT NULL,
            PRIMARY KEY(formula)
        )
        """.trimIndent(),
        """
        CREATE TABLE IF NOT EXISTS game_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            score INTEGER NOT NULL,
            success INTEGER NOT NULL,
            difficulty TEXT NOT NULL,
            missionFormula TEXT,
            missionTargetCount INTEGER,
            playedAt INTEGER NOT NULL,
            moleculesMade TEXT NOT NULL
        )
        """.trimIndent(),
        "CREATE INDEX IF NOT EXISTS index_game_molecule_discoveries_lastDiscoveredAt ON game_molecule_discoveries(lastDiscoveredAt)",
        "CREATE INDEX IF NOT EXISTS index_game_molecule_discoveries_discoveryCount ON game_molecule_discoveries(discoveryCount)",
        "CREATE INDEX IF NOT EXISTS index_game_sessions_playedAt ON game_sessions(playedAt)",
        "CREATE INDEX IF NOT EXISTS index_game_sessions_score ON game_sessions(score)",
        "CREATE INDEX IF NOT EXISTS index_game_sessions_difficulty ON game_sessions(difficulty)",
    )

    internal val MIGRATION_5_6_SQL: List<String> = listOf(
        "ALTER TABLE game_sessions ADD COLUMN mode TEXT NOT NULL DEFAULT 'MISSION'",
        "CREATE INDEX IF NOT EXISTS index_game_sessions_mode ON game_sessions(mode)",
    )

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE elements ADD COLUMN latin_name TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN latin_pronunciation TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN english_pronunciation TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN discovery_year INTEGER")
            db.execSQL("ALTER TABLE elements ADD COLUMN discoverer TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN discovery_country TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN cost_per_100g_usd REAL")
            db.execSQL("ALTER TABLE elements ADD COLUMN cost_reference_date TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN cas_number TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN pubchem_cid TEXT")
            db.execSQL("ALTER TABLE elements ADD COLUMN rtecs_number TEXT")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS isotopes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    atomic_number INTEGER NOT NULL,
                    mass_number INTEGER NOT NULL,
                    neutron_count INTEGER NOT NULL,
                    symbol TEXT NOT NULL,
                    is_stable INTEGER NOT NULL,
                    half_life TEXT,
                    half_life_seconds REAL,
                    decay_mode TEXT,
                    natural_abundance REAL,
                    application_tags TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_isotopes_atomic_number ON isotopes(atomic_number)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_isotopes_symbol ON isotopes(symbol)")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val alterColumns = listOf(
                "ALTER TABLE elements ADD COLUMN proton_count INTEGER",
                "ALTER TABLE elements ADD COLUMN neutron_count INTEGER",
                "ALTER TABLE elements ADD COLUMN electron_count INTEGER",
                "ALTER TABLE elements ADD COLUMN electron_shells TEXT",
                "ALTER TABLE elements ADD COLUMN common_ion_charge TEXT",
                "ALTER TABLE elements ADD COLUMN ionization_potential REAL",
                "ALTER TABLE elements ADD COLUMN ionization_possibility TEXT",
                "ALTER TABLE elements ADD COLUMN covalent_radius REAL",
                "ALTER TABLE elements ADD COLUMN vdw_radius REAL",
                "ALTER TABLE elements ADD COLUMN block_name TEXT",
                "ALTER TABLE elements ADD COLUMN melting_point REAL",
                "ALTER TABLE elements ADD COLUMN boiling_point REAL",
                "ALTER TABLE elements ADD COLUMN heat_fusion REAL",
                "ALTER TABLE elements ADD COLUMN liquid_density REAL",
                "ALTER TABLE elements ADD COLUMN electrical_conductivity REAL",
                "ALTER TABLE elements ADD COLUMN electrical_type TEXT",
                "ALTER TABLE elements ADD COLUMN resistivity REAL",
                "ALTER TABLE elements ADD COLUMN superconducting_temp REAL",
                "ALTER TABLE elements ADD COLUMN magnetism TEXT",
                "ALTER TABLE elements ADD COLUMN mag_suscept_volume REAL",
                "ALTER TABLE elements ADD COLUMN mag_suscept_mass REAL",
                "ALTER TABLE elements ADD COLUMN mag_suscept_molar REAL",
                "ALTER TABLE elements ADD COLUMN crystal_structure TEXT",
                "ALTER TABLE elements ADD COLUMN crystal_system TEXT",
                "ALTER TABLE elements ADD COLUMN lattice_a REAL",
                "ALTER TABLE elements ADD COLUMN lattice_b REAL",
                "ALTER TABLE elements ADD COLUMN lattice_c REAL",
                "ALTER TABLE elements ADD COLUMN lattice_alpha REAL",
                "ALTER TABLE elements ADD COLUMN lattice_beta REAL",
                "ALTER TABLE elements ADD COLUMN lattice_gamma REAL",
                "ALTER TABLE elements ADD COLUMN crystal_habit TEXT",
                "ALTER TABLE elements ADD COLUMN debye_temp REAL",
                "ALTER TABLE elements ADD COLUMN hardness_brinell REAL",
                "ALTER TABLE elements ADD COLUMN hardness_mohs REAL",
                "ALTER TABLE elements ADD COLUMN hardness_vickers REAL",
                "ALTER TABLE elements ADD COLUMN bulk_modulus REAL",
                "ALTER TABLE elements ADD COLUMN young_modulus REAL",
                "ALTER TABLE elements ADD COLUMN poisson_ratio REAL",
                "ALTER TABLE elements ADD COLUMN shear_modulus REAL",
                "ALTER TABLE elements ADD COLUMN speed_of_sound REAL",
                "ALTER TABLE elements ADD COLUMN refractive_index REAL",
                "ALTER TABLE elements ADD COLUMN electron_affinity REAL",
                "ALTER TABLE elements ADD COLUMN electrode_potential REAL",
                "ALTER TABLE elements ADD COLUMN radioactivity_level TEXT",
                "ALTER TABLE elements ADD COLUMN reactivity_level TEXT",
                "ALTER TABLE elements ADD COLUMN hazard_health INTEGER",
                "ALTER TABLE elements ADD COLUMN hazard_flammability INTEGER",
                "ALTER TABLE elements ADD COLUMN hazard_reactivity INTEGER",
                "ALTER TABLE elements ADD COLUMN hazard_special TEXT",
                "ALTER TABLE elements ADD COLUMN abundance_universe REAL",
                "ALTER TABLE elements ADD COLUMN abundance_sun REAL",
                "ALTER TABLE elements ADD COLUMN abundance_ocean REAL",
                "ALTER TABLE elements ADD COLUMN abundance_human REAL",
                "ALTER TABLE elements ADD COLUMN abundance_crust REAL",
                "ALTER TABLE elements ADD COLUMN abundance_meteorite REAL",
                "ALTER TABLE elements ADD COLUMN data_source TEXT",
                "ALTER TABLE elements ADD COLUMN data_license TEXT",
                "ALTER TABLE elements ADD COLUMN data_updated_at TEXT",
                "ALTER TABLE elements ADD COLUMN data_confidence REAL"
            )
            alterColumns.forEach { db.execSQL(it) }
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_4_5_SQL.forEach { statement -> db.execSQL(statement) }
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_5_6_SQL.forEach { statement -> db.execSQL(statement) }
        }
    }
}
