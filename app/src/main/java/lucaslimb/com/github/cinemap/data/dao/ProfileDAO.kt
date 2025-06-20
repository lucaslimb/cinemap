package lucaslimb.com.github.cinemap.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import lucaslimb.com.github.cinemap.data.models.Profile
import lucaslimb.com.github.cinemap.data.models.ProfileSavedMovie
import lucaslimb.com.github.cinemap.data.models.SavedMovie

@Dao
interface ProfileDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: Profile)

    @Update
    suspend fun updateUserProfile(profile: Profile)

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun getProfile(): Profile?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun saveMovie(movie: SavedMovie): Long

    @Delete
    suspend fun deleteMovie(movie: SavedMovie)

    @Query("SELECT * FROM saved_movie WHERE profileId = :profileId ORDER BY country DESC")
    fun getSavedMovieForProfile(profileId: Int): Flow<List<SavedMovie>>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_movie WHERE movieId = :movieId AND profileId = :profileId LIMIT 1)")
    suspend fun isMovieSaved(movieId: Int, profileId: Int): Boolean

    @Transaction // Garante que a operação seja atômica
    @Query("SELECT * FROM profile WHERE id = 1")
    fun getUserProfileWithMovies(): Flow<ProfileSavedMovie?>

    @Query("SELECT title FROM profile WHERE id = 1")
    suspend fun getTitle(): String

    @Query("SELECT COUNT(*) FROM saved_movie WHERE profileId = 1")
    suspend fun getFilmsCount(): Int

    @Query("SELECT COUNT(DISTINCT country) FROM saved_movie WHERE profileId = 1 AND country IS NOT NULL AND country != ''")
    suspend fun getCountriesCount(): Int

    @Query("SELECT continentsCount FROM profile WHERE id = 1")
    suspend fun getContinentsCount(): Int

    @Query("SELECT COUNT(DISTINCT releaseYear) FROM saved_movie WHERE profileId = 1 AND releaseYear IS NOT NULL AND releaseYear != ''")
    suspend fun getYearsCount(): Int

}