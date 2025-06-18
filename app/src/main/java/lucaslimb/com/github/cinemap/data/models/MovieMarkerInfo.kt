package lucaslimb.com.github.cinemap.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MovieMarkerInfo(
    val movieId: Int,
    val originalTitle: String,
    val director: String,
    val duration: Int?,
    val mainGenre: String?,
    val posterUrl: String?,
    val country: String?,
    val releaseYear: Int?
) : Parcelable