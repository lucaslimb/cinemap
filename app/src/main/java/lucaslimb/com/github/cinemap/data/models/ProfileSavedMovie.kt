package lucaslimb.com.github.cinemap.data.models

import androidx.room.Embedded
import androidx.room.Relation

data class ProfileSavedMovie(
    @Embedded
    val userProfile: Profile,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val savedMovies: List<SavedMovie>
)