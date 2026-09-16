package dev.vantafyn.feature.home.guide

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class WatchGuideDatabase(
    context: Context,
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_UNLOCKED_GUIDES)
        db.execSQL(CREATE_UNLOCKED_INDEX)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        // Future schema migrations
    }

    companion object {
        const val DATABASE_NAME = "vantafyn_watch_guides.db"
        const val DATABASE_VERSION = 1

        private const val CREATE_UNLOCKED_GUIDES = """
            CREATE TABLE unlocked_guides (
                code TEXT NOT NULL PRIMARY KEY,
                unlocked_at INTEGER NOT NULL
            )
        """

        private const val CREATE_UNLOCKED_INDEX = """
            CREATE INDEX idx_unlocked_guides_code ON unlocked_guides(code)
        """
    }
}
