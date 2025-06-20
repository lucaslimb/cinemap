package lucaslimb.com.github.cinemap.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "saved_movie",
    foreignKeys = [ForeignKey(
        entity = Profile::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["profileId", "movieId"], unique = true)]
)
data class SavedMovie(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Int,
    val movieId: Int,
    val title: String,
    val posterUrl: String?,
    val director: String,
    val country: String?,
    val releaseYear: Int?,
    val tagline: String?
)