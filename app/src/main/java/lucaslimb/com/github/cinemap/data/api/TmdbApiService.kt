package lucaslimb.com.github.cinemap.data.api

import lucaslimb.com.github.cinemap.data.models.MovieCreditsResponse
import lucaslimb.com.github.cinemap.data.models.MovieDetailsResponse
import lucaslimb.com.github.cinemap.data.models.MovieSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US"
    ): MovieSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US" // Pode mudar para "pt-BR"
    ): MovieDetailsResponse

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String
    ): MovieCreditsResponse

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("primary_release_year") year: Int,
        @Query("with_original_language") originalLanguage: String? = null,
        @Query("with_origin_country") countryCode: String,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1
    ): MovieSearchResponse
}