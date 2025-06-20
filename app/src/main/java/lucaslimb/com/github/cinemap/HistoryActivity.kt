package lucaslimb.com.github.cinemap

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lucaslimb.com.github.cinemap.adapters.SavedMovieAdapter
import lucaslimb.com.github.cinemap.data.db.AppDatabase

class HistoryActivity : AppCompatActivity() {

    private lateinit var dao: lucaslimb.com.github.cinemap.data.dao.ProfileDAO
    private lateinit var recyclerViewSavedFilms: RecyclerView
    private lateinit var savedMovieAdapter: SavedMovieAdapter
    private lateinit var title: TextView
    private lateinit var filmsCount: TextView
    private lateinit var countriesCount: TextView
    private lateinit var yearsCount: TextView
    private lateinit var tvOneliner: TextView
    private val pagerSnapHelper = PagerSnapHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dao = AppDatabase.getDatabase(this).profileDao()

        val toolbar: Toolbar = findViewById(R.id.toolbar_history)
        configureToolbar(toolbar)
        val btnBack: ImageButton = findViewById(R.id.toolbar_history_btn_back)
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val btnConfig: ImageButton = findViewById(R.id.toolbar_history_btn_settings)
        btnConfig.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }

        title = findViewById(R.id.tv_history_profile_title)
        filmsCount = findViewById(R.id.tv_history_films_count)
        countriesCount = findViewById(R.id.tv_history_countries_count)
        yearsCount = findViewById(R.id.tv_history_years_count)
        recyclerViewSavedFilms = findViewById(R.id.recycler_view_saved_films)
        tvOneliner = findViewById<TextView>(R.id.tv_sf_oneliner)

        savedMovieAdapter = SavedMovieAdapter(dao, lifecycleScope)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewSavedFilms.layoutManager = layoutManager
        recyclerViewSavedFilms.adapter = savedMovieAdapter
        pagerSnapHelper.attachToRecyclerView(recyclerViewSavedFilms)

        savedMovieAdapter.onMovieDeletedCallback = {
            lifecycleScope.launch {
                updateUserProfileCounts()
            }
        }
        recyclerViewSavedFilms.post {
            val recyclerViewWidthPx = recyclerViewSavedFilms.width
            val displayMetrics = resources.displayMetrics
            val itemWidthDp = 150f
            val calculatedItemWidthPx = (itemWidthDp * displayMetrics.density).toInt()
            val horizontalPadding = (recyclerViewWidthPx / 2) - (calculatedItemWidthPx / 2)
            recyclerViewSavedFilms.setPadding(horizontalPadding, 0, horizontalPadding, 0)
            recyclerViewSavedFilms.clipToPadding = false
        }
        recyclerViewSavedFilms.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updatePrincipalMovieInfo()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updatePrincipalMovieInfo()
                }
            }
        })

        recyclerViewSavedFilms.post {
            updatePrincipalMovieInfo()
        }

        loadProfileInfo()

    }

    private fun updatePrincipalMovieInfo() {
        val layoutManager = recyclerViewSavedFilms.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val snapView = pagerSnapHelper.findSnapView(it) // Usando a instância única

            if (snapView != null) {
                val snappedPosition = it.getPosition(snapView)
                if (snappedPosition != RecyclerView.NO_POSITION) {
                    val principalMovie = savedMovieAdapter.getMovieAtPosition(snappedPosition)
                    tvOneliner.text = principalMovie.tagline ?: ""
                } else {
                    tvOneliner.text = ""
                }
            } else {
                tvOneliner.text = ""
            }
        }
    }

    private fun configureToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.color.background))
    }

    private suspend fun updateUserProfileCounts() {
        withContext(Dispatchers.IO) {
            val title = dao.getTitle()
            val filmsCount = dao.getFilmsCount()
            val countriesCount = dao.getCountriesCount()
            val continentsCount = dao.getContinentsCount()
            val yearsCount = dao.getYearsCount()

            val currentProfile = dao.getProfile()
            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(
                    title = title,
                    filmsCount = filmsCount,
                    countriesCount = countriesCount,
                    continentsCount = continentsCount,
                    yearsCount = yearsCount
                )
                dao.updateUserProfile(updatedProfile)
            }
        }
        loadProfileInfo()
    }

    private fun loadProfileInfo(){
        lifecycleScope.launch {
            val numberFilms = dao.getFilmsCount()

            if (dao.getTitle() != "null") {
                val titleLevel = when {
                    numberFilms == 0 -> R.string.tv_history_profile_0
                    numberFilms <= 9 -> R.string.tv_history_profile_10
                    numberFilms <= 19 -> R.string.tv_history_profile_20
                    numberFilms <= 39 -> R.string.tv_history_profile_40
                    numberFilms <= 69 -> R.string.tv_history_profile_70
                    numberFilms <= 99 -> R.string.tv_history_profile_99
                    else -> R.string.tv_history_profile_100
                }
                title.text = getString(R.string.tv_history_profile_title, getString(titleLevel))
            }

            filmsCount.text = getString(R.string.tv_history_films_count, numberFilms)
            countriesCount.text = getString(R.string.tv_history_countries_count, dao.getCountriesCount())
            yearsCount.text = getString(R.string.tv_history_years_count, dao.getYearsCount())

            dao.getSavedMovieForProfile(1).collectLatest { savedMovies ->
                savedMovieAdapter.submitList(savedMovies)
            }

        }
    }

}
