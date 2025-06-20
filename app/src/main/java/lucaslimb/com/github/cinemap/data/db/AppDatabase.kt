package lucaslimb.com.github.cinemap.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lucaslimb.com.github.cinemap.data.dao.ProfileDAO
import lucaslimb.com.github.cinemap.data.models.Profile
import lucaslimb.com.github.cinemap.data.models.SavedMovie

@Database(entities = [Profile::class, SavedMovie::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cinemap_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(ProfileDatabaseCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ProfileDatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val profileDao = database.profileDao()

                        val existingProfile = profileDao.getProfile()

                        if (existingProfile == null) {
                            val defaultProfile = Profile(
                                id = 1,
                                title = "Awaiting your next journey",
                                filmsCount = 0,
                                countriesCount = 0,
                                continentsCount = 0,
                                yearsCount = 0
                            )
                            profileDao.insertUserProfile(defaultProfile)
                        }
                    }
                }
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val profileDao = database.profileDao()
                        val existingProfile = profileDao.getProfile()
                        if (existingProfile == null) {
                            val defaultProfile = Profile(
                                id = 1,
                                title = "Awaiting your next journey",
                                filmsCount = 0,
                                countriesCount = 0,
                                continentsCount = 0,
                                yearsCount = 0
                            )
                            profileDao.insertUserProfile(defaultProfile)
                        }
                    }
                }
            }
        }

    }
}