package lucaslimb.com.github.cinemap.data.models

import com.google.gson.annotations.SerializedName

data class MovieSearchResponse(
    val page: Int,
    val results: List<MovieSearchResult>,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("total_results") val totalResults: Int
)

data class MovieSearchResult(
    val id: Int,
    val title: String,
    @SerializedName("original_title") val original_title: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("poster_path") val posterPath: String?,
    val overview: String?,
)