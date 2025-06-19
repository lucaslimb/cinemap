package lucaslimb.com.github.cinemap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.Resources
import android.location.Geocoder
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.LatLngBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import lucaslimb.com.github.cinemap.views.TimelineSliderView
import lucaslimb.com.github.cinemap.data.api.RetrofitClient
import lucaslimb.com.github.cinemap.data.db.AppDatabase
import lucaslimb.com.github.cinemap.data.models.MovieCreditsResponse
import lucaslimb.com.github.cinemap.data.models.MovieDetailsResponse
import lucaslimb.com.github.cinemap.data.models.MovieMarkerInfo
import lucaslimb.com.github.cinemap.data.models.MovieSearchResponse
import lucaslimb.com.github.cinemap.utils.Constants
import java.io.IOException
import java.util.Locale
import kotlin.math.hypot

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,  GoogleMap.OnCameraIdleListener {

    private lateinit var dao: lucaslimb.com.github.cinemap.data.dao.ProfileDAO
    private var anoNovo: Int = 0
    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var geocoder: Geocoder

    private lateinit var soundPool: SoundPool
    private var sliderSoundId: Int = 0
    private var markerSoundId: Int = 0

    private val discoveredMovies = mutableListOf<MovieMarkerInfo>()
    private var lastSearchedYear: Int? = null
    private var lastSearchedCountryCode: String? = null
    private var currentDiscoverResponse: MovieSearchResponse? = null
    private var currentMoviePageIndex: Int = 0
    private val pageSize = 5
    private var tmdbPageNumber: Int = 1
    private var lastSearchedBounds: LatLngBounds? = null
    private var currentSearchCenter: LatLng? = null
    private var originalLang: Boolean = true
    private var showSaved: Boolean = false

    private companion object {
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
        private const val TMDB_API_KEY = BuildConfig.TMDB_API_KEY
        private const val POSTER_TARGET_WIDTH = 300
        private const val POSTER_TARGET_HEIGHT = 450
        private const val MOVEMENT_THRESHOLD_DEGREES = 0.5
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dao = AppDatabase.getDatabase(this).profileDao()
        geocoder = Geocoder(this, Locale.getDefault())

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        configureToolbar(toolbar)
        val btnHistory: ImageButton = findViewById(R.id.toolbar_btn_profile)
        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        val btnSearch: ImageButton = findViewById(R.id.btn_search)
        btnSearch.setOnClickListener {
            val countrySearched = findViewById<EditText>(R.id.et_country_search).text.toString().trim()
            lifecycleScope.launch {
                searchCountry(countrySearched)
            }
        }

        val sharedPrefs = getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE)

        val selectedLangId = sharedPrefs.getInt(Constants.PREF_KEY_LANG_SELECTION, R.id.cb_config_lang_inter)
        when (selectedLangId) {
            R.id.cb_config_lang_original -> {
                originalLang = true
            }
            R.id.cb_config_lang_inter -> {
                originalLang = false
            }
        }

        val selectedSavedId = sharedPrefs.getInt(Constants.PREF_KEY_SAVED_SELECTION, R.id.cb_config_saved_no)
        when (selectedSavedId) {
            R.id.cb_config_saved_yes -> {
                showSaved = true
            }
            R.id.cb_config_saved_no -> {
                showSaved = false
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        sliderSoundId = soundPool.load(this, R.raw.pop_slider, 1)
        markerSoundId = soundPool.load(this, R.raw.pop_marker, 1)

        val timelineSlider: TimelineSliderView = findViewById(R.id.timelineSlider)
        val yearDisplay: TextView = findViewById(R.id.tv_year)

        timelineSlider.onYearSelected = { year ->
            yearDisplay.text = year.toString()
            soundPool.play(sliderSoundId, 0.1f, 0.1f, 0, 0, 1f)
        }

        timelineSlider.onYearSettled = { year ->
            anoNovo = year
            Log.d(TAG, "Ano alterado para: $anoNovo. Limpando tudo e iniciando nova busca.")
            lastSearchedYear = null
            lastSearchedCountryCode = null
            currentDiscoverResponse = null
            currentMoviePageIndex = 0
            tmdbPageNumber = 1
            lastSearchedBounds = null
            currentSearchCenter = null

            discoveredMovies.clear()
            if (::googleMap.isInitialized) {
                googleMap.clear()
            }

            triggerMovieSearch()
        }

        mapView = findViewById(R.id.map_view)
        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
        mapView.onCreate(mapViewBundle)
        mapView.getMapAsync(this)
    }

    private suspend fun searchCountry(countrySearched: String) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
            getLatLngFromCountryName(countrySearched)?: LatLng(11.1779, -35.212),
            5f))
    }

    private suspend fun getLatLngFromCountryName(countryName: String): LatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocationName(countryName, 5)
                if (!addresses.isNullOrEmpty()) {
                    for (address in addresses) {
                        Log.d(TAG, "Resultado encontrado: ${address.featureName}, ${address.locality}, ${address.countryName}, lat: ${address.latitude}, lon: ${address.longitude}")
                    }
                    val selected = addresses[0]
                    LatLng(selected.latitude, selected.longitude)
                } else {
                    Log.w(TAG, "Nenhum endereço encontrado para: $countryName")
                    null
                }
            } catch (e: IOException) {
                Log.e(TAG, "Erro de IO na geocodificação: ${e.message}", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado na geocodificação: ${e.message}", e)
                null
            }
        }
    }

    private fun configureToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.color.background))
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "Mapa pronto!")

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(11.1779, -35.212), 1f))

        try {
            googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style)
            )
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Não foi possível encontrar o estilo. Erro: ", e)
        }

        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnCameraIdleListener(this)
    }

    override fun onCameraIdle() {
        triggerMovieSearch()
    }

    private fun triggerMovieSearch() {
        if (!::googleMap.isInitialized) {
            Log.w(TAG, "googleMap não inicializada, não é possível disparar a busca.")
            return
        }

        val currentLatLng = googleMap.cameraPosition.target
        val currentYear = anoNovo
        val currentBounds = googleMap.projection.visibleRegion.latLngBounds

        lifecycleScope.launch(Dispatchers.Main) {
            val countryCode = getCountryCodeFromLatLng(currentLatLng)
            if (countryCode.isNullOrEmpty()) {
                Log.w(TAG, "Não foi possível determinar o código do país para $currentLatLng")
                return@launch
            }

            val yearChanged = (currentYear != lastSearchedYear)
            val countryChanged = (countryCode != lastSearchedCountryCode)
            val initialLoad = (currentDiscoverResponse == null)

            var fetchNewTmdbPage = false

            val btnClear: Button = findViewById(R.id.btn_clearmap)
            btnClear.setOnClickListener{
                currentMoviePageIndex = 0
                tmdbPageNumber = 1
                lastSearchedBounds = null
                currentSearchCenter = null
                discoveredMovies.clear()
                googleMap.clear()
            }

            if (yearChanged || initialLoad) {
                Log.d(TAG, "CENÁRIO 1 (Ano mudou ou Carga Inicial): Limpando tudo e fazendo nova API call.")
                tmdbPageNumber = 1
                currentMoviePageIndex = 0
                discoveredMovies.clear()
                googleMap.clear()
                fetchNewTmdbPage = true
            } else if (countryChanged) {
                Log.d(TAG, "CENÁRIO 2 (Ano igual, País mudou): Fazendo nova API call para novo país, sem limpar marcadores existentes.")
                tmdbPageNumber = 1
                currentMoviePageIndex = 0
                fetchNewTmdbPage = true
            } else {
                val totalResultsOnCurrentTmdbPage = currentDiscoverResponse?.results?.size ?: 0
                val allMoviesFromCurrentTmdbPageProcessed = (currentMoviePageIndex * pageSize >= totalResultsOnCurrentTmdbPage)
                val movedSignificantly = currentSearchCenter != null &&
                        hypot(
                            currentLatLng.latitude - currentSearchCenter!!.latitude,
                            currentLatLng.longitude - currentSearchCenter!!.longitude
                        ) > MOVEMENT_THRESHOLD_DEGREES


                if (allMoviesFromCurrentTmdbPageProcessed && movedSignificantly) {
                    Log.d(TAG, "CENÁRIO 3a (Ano e País iguais, todos os filmes da página atual processados E usuário saiu da área): Carregando próxima página da API.")
                    tmdbPageNumber++
                    currentMoviePageIndex = 0
                    fetchNewTmdbPage = true
                } else if (!allMoviesFromCurrentTmdbPageProcessed) {
                    Log.d(TAG, "CENÁRIO 3b (Ano e País iguais, filmes da página atual ainda não processados): Carregando próximos $pageSize filmes da resposta cacheada.")
                } else {
                    Log.d(TAG, "CENÁRIO 3c (Ano e País iguais, todos os filmes da página atual processados, mas usuário NÃO saiu da área): Não há mais filmes para carregar nesta área.")
                    return@launch
                }
            }

            lastSearchedYear = currentYear
            lastSearchedCountryCode = countryCode
            currentSearchCenter = currentLatLng

            var moviesToProcess: List<lucaslimb.com.github.cinemap.data.models.MovieSearchResult>

            if (fetchNewTmdbPage) {
                try {
                    val discoverResponse = RetrofitClient.tmdbApiService.discoverMovies(
                        apiKey = TMDB_API_KEY,
                        year = currentYear,
                        countryCode = countryCode,
                        page = tmdbPageNumber
                    )
                    currentDiscoverResponse = discoverResponse
                    lastSearchedBounds = currentBounds
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao buscar filmes da API para página $tmdbPageNumber: ${e.message}", e)
                    currentDiscoverResponse = null
                    moviesToProcess = emptyList()
                }
            }

            val startIndex = currentMoviePageIndex * pageSize
            val totalResults = currentDiscoverResponse?.results?.size ?: 0
            val endIndex = minOf(startIndex + pageSize, totalResults)

            if (startIndex >= totalResults) {
                Log.d(TAG, "Nenhum filme encontrado para a página atual ou critério. Ou todos os filmes da página TMDB foram processados.")
                moviesToProcess = emptyList()
                if (fetchNewTmdbPage && currentDiscoverResponse?.results.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, "No more films found for this year and region.", Toast.LENGTH_SHORT).show()
                }
            } else {
                moviesToProcess = currentDiscoverResponse!!.results.subList(startIndex, endIndex)
                currentMoviePageIndex++
            }

            if (moviesToProcess.isEmpty()) {
                Log.w(TAG, "Nenhum filme a processar para a página atual ou critérios especificados.")
                return@launch
            }

            for ((indexInBatch, movieFound) in moviesToProcess.withIndex()) {
                if (discoveredMovies.any { it.movieId == movieFound.id }){
                    Log.d(TAG, "Filme '${movieFound.original_title}' (ID: ${movieFound.id}) já está na lista 'discoveredMovies'. Ignorando.")
                    continue
                }

                try {
                    val movieDetails: MovieDetailsResponse = RetrofitClient.tmdbApiService.getMovieDetails(
                        movieId = movieFound.id,
                        apiKey = TMDB_API_KEY,
                        language = "en-US"
                    )

                    val movieCredits: MovieCreditsResponse = RetrofitClient.tmdbApiService.getMovieCredits(
                        movieId = movieFound.id,
                        apiKey = TMDB_API_KEY
                    )

                    val director = movieCredits.crew.firstOrNull { it.job == "Director" }?.name ?: "N/A"
                    var originalTitle = movieDetails.originalTitle
                    if(!originalLang) {
                        originalTitle = movieDetails.title
                    }
                    val posterPath = movieDetails.posterPath
                    val duration = movieDetails.runtime
                    val mainGenre = movieDetails.genres.firstOrNull()?.name ?: "N/A"
                    val tagline = movieDetails.tagline ?: "N/A"
                    val countryName = movieDetails.productionCountries?.firstOrNull().toString()
                    val releaseYear = movieDetails.releaseDate.split("-").firstOrNull()?.toIntOrNull()
                    val isAdult = movieDetails.adult

                    val fullPosterUrl = RetrofitClient.getPosterUrl(posterPath)

                    val movieMarkerInfo = MovieMarkerInfo(
                        movieId = movieFound.id,
                        originalTitle = originalTitle,
                        director = director,
                        duration = duration,
                        mainGenre = mainGenre,
                        posterUrl = fullPosterUrl,
                        country = countryName,
                        releaseYear = releaseYear,
                        tagline = tagline
                    )

                    if(!isAdult && duration != 0 && posterPath != null) {
                        if(!showSaved) {
                            if(!dao.isMovieSaved(movieFound.id, 1)){
                                discoveredMovies.add(movieMarkerInfo)
                                addSingleMovieMarker(movieMarkerInfo, currentLatLng, indexInBatch)
                            } else{
                                continue
                            }
                        } else{
                            discoveredMovies.add(movieMarkerInfo)
                            addSingleMovieMarker(movieMarkerInfo, currentLatLng, indexInBatch)
                        }
                        continue
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao buscar detalhes ou créditos para filme ID ${movieFound.id}: ${e.message}", e)
                }
            }
        }
    }

    private suspend fun getCountryCodeFromLatLng(latLng: LatLng): String? {
        return withContext(Dispatchers.IO) {
            try {
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val countryCode = addresses[0].countryCode
                    Log.d(TAG, "País para ${latLng.latitude}, ${latLng.longitude}: $countryCode")
                    countryCode
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na geocodificação reversa: ${e.message}", e)
                null
            }
        }
    }

    private fun addSingleMovieMarker(movieInfo: MovieMarkerInfo, searchCenterLatLng: LatLng, markerIndex: Int) {
        lifecycleScope.launch(Dispatchers.Main) {
            val markerPosition = getOffsetLatLng(searchCenterLatLng, markerIndex)
            Log.d(TAG, "addSingleMovieMarker: Movie ${movieInfo.originalTitle}, Index: $markerIndex, Original LatLng: (${searchCenterLatLng.latitude}, ${searchCenterLatLng.longitude}), Offset LatLng: (${markerPosition.latitude}, ${markerPosition.longitude})")

            val customMarkerOptions = createCustomMarkerOptions(
                this@MainActivity,
                movieInfo.posterUrl,
                markerPosition
            )
            val marker = googleMap.addMarker(customMarkerOptions)
            marker?.tag = movieInfo
            marker?.title = movieInfo.originalTitle

            soundPool.play(markerSoundId, 0.1f, 0.1f, 0, 0, 1f)
        }
    }

    private fun getOffsetLatLng(baseLatLng: LatLng, index: Int): LatLng {
        val deltaLat = 1
        val deltaLng = 1.3

        return when (index) {
            0 -> baseLatLng
            1 -> LatLng(baseLatLng.latitude + deltaLat, baseLatLng.longitude)
            2 -> LatLng(baseLatLng.latitude - deltaLat, baseLatLng.longitude)
            3 -> LatLng(baseLatLng.latitude, baseLatLng.longitude + deltaLng)
            4 -> LatLng(baseLatLng.latitude, baseLatLng.longitude - deltaLng)
            else -> baseLatLng
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val movieInfo = marker.tag as? MovieMarkerInfo
        if (movieInfo != null) {
            val dialog = MovieDetailsDialogFragment.newInstance(movieInfo)
            dialog.show(supportFragmentManager, "MovieDetailsDialog")
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    //testar aqui o cleaning TODO
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
        discoveredMovies.clear()
        currentDiscoverResponse = null
        lastSearchedBounds = null
        currentSearchCenter = null
        soundPool.release()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private suspend fun createCustomMarkerOptions(
        context: Context,
        imageUrl: String?,
        position: LatLng
    ): MarkerOptions = suspendCancellableCoroutine { continuation ->
        val markerView = LayoutInflater.from(context).inflate(R.layout.custom_marker_brown, null)
        val markerImage = markerView.findViewById<ImageView>(R.id.marker_image)

        val cornerRadiusDp = 8f
        val cornerRadiusPx = (context.resources.displayMetrics.density * cornerRadiusDp).toInt()

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions.bitmapTransform(RoundedCorners(cornerRadiusPx)))
                .override(POSTER_TARGET_WIDTH, POSTER_TARGET_HEIGHT)
                .centerCrop()
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        markerImage.setImageBitmap(resource)
                        val bitmapDescriptor = createBitmapDescriptorFromView(context, markerView)
                        continuation.resume(
                            MarkerOptions()
                                .position(position)
                                .icon(bitmapDescriptor)
                                .anchor(0.5f, 1.0f)
                        )
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                        markerImage.setImageResource(R.drawable.placeholder_poster)
                        val bitmapDescriptor = createBitmapDescriptorFromView(context, markerView)
                        continuation.resume(
                            MarkerOptions()
                                .position(position)
                                .icon(bitmapDescriptor)
                                .anchor(0.5f, 1.0f)
                        )
                    }
                })
        } else {
            markerImage.setImageResource(R.drawable.placeholder_poster)
            val bitmapDescriptor = createBitmapDescriptorFromView(context, markerView)
            continuation.resume(
                MarkerOptions()
                    .position(position)
                    .icon(bitmapDescriptor)
                    .anchor(0.5f, 1.0f)
            )
        }
    }

    private fun createBitmapDescriptorFromView(context: Context, view: View): BitmapDescriptor {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val width = view.measuredWidth
        val height = view.measuredHeight
        view.layout(0, 0, width, height)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
