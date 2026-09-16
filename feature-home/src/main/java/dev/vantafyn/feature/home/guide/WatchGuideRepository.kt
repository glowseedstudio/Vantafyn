package dev.vantafyn.feature.home.guide

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase

class WatchGuideRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dbHelper = WatchGuideDatabase(appContext)

    init {
        migrateFromSharedPreferencesIfPresent()
    }

    private fun migrateFromSharedPreferencesIfPresent() {
        try {
            val prefs = appContext.getSharedPreferences("vantafyn_unlockables", Context.MODE_PRIVATE)
            val legacyCodes = prefs.getStringSet("unlocked_codes", null)
            if (!legacyCodes.isNullOrEmpty()) {
                unlockCodes(legacyCodes)
            }
        } catch (_: Exception) {
            // Best effort migration
        }
    }

    fun getUnlockedCodes(): Set<String> {
        val result = mutableSetOf<String>()
        try {
            val db = dbHelper.readableDatabase
            db.query(
                "unlocked_guides",
                arrayOf("code"),
                null,
                null,
                null,
                null,
                null,
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(0))
                }
            }
        } catch (_: Exception) {
            // Return empty or fallback
        }
        return result
    }

    fun isCodeUnlocked(code: String): Boolean {
        try {
            val db = dbHelper.readableDatabase
            db.query(
                "unlocked_guides",
                arrayOf("code"),
                "code = ?",
                arrayOf(code.trim().uppercase()),
                null,
                null,
                null,
            ).use { cursor ->
                return cursor.moveToFirst()
            }
        } catch (_: Exception) {
            return false
        }
    }

    fun unlockCode(code: String) {
        unlockCodes(listOf(code))
    }

    fun unlockCodes(codes: Collection<String>) {
        if (codes.isEmpty()) return
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                val now = System.currentTimeMillis()
                for (code in codes) {
                    val normalized = code.trim().uppercase()
                    if (normalized.isNotBlank()) {
                        val values = ContentValues().apply {
                            put("code", normalized)
                            put("unlocked_at", now)
                        }
                        db.insertWithOnConflict(
                            "unlocked_guides",
                            null,
                            values,
                            SQLiteDatabase.CONFLICT_IGNORE,
                        )
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        } catch (_: Exception) {
            // Best effort write
        }
    }
}
