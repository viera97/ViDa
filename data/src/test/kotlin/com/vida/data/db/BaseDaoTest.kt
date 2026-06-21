package com.vida.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before

/**
 * Base class for DAO integration tests using Room in-memory database.
 *
 * Subclasses override [setUp] and call `super.setUp()` if they need
 * to inject additional setup before the database is created.
 */
abstract class BaseDaoTest {
    protected lateinit var database: AppDatabase

    @Before
    open fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    open fun tearDown() {
        database.close()
    }
}
