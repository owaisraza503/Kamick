package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KamickDatabase
import com.example.data.local.LocalContentManager
import com.example.data.model.CategoryEntity
import com.example.data.model.MangaEntity
import com.example.data.repository.MangaRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var db: KamickDatabase
  private lateinit var repository: MangaRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, KamickDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val contentManager = LocalContentManager(context)
    repository = MangaRepository(context, db.kamickDao(), contentManager)
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Kamick", appName)
  }

  @Test
  fun `database initialization creates default categories and sample manga`() = runBlocking {
    repository.initializeDefaults()
    val categories = repository.allCategories.first()
    assertTrue(categories.any { it.name == "All" })
    assertTrue(categories.any { it.name == "Favorites" })

    val mangas = repository.allMangas.first()
    assertTrue("Should have initialized sample mangas", mangas.isNotEmpty())
    val firstManga = mangas.first()
    assertNotNull(firstManga.title)
  }

  @Test
  fun `toggle favorite updates manga favorite status`() = runBlocking {
    repository.initializeDefaults()
    val mangas = repository.allMangas.first()
    val firstManga = mangas.first()
    val initialFav = firstManga.favorite

    repository.toggleFavorite(firstManga.id)
    val updatedManga = repository.getManga(firstManga.id).first()
    assertEquals(!initialFav, updatedManga?.favorite)
  }
}
