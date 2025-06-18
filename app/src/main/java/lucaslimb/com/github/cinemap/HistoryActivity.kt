package lucaslimb.com.github.cinemap

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import lucaslimb.com.github.cinemap.adapters.SavedMovieAdapter
import lucaslimb.com.github.cinemap.data.db.AppDatabase

class HistoryActivity : AppCompatActivity() {

    private lateinit var dao: lucaslimb.com.github.cinemap.data.dao.ProfileDAO
    private lateinit var recyclerViewSavedFilms: RecyclerView
    private lateinit var savedMovieAdapter: SavedMovieAdapter
    private lateinit var title: TextView
    private lateinit var filmsCount: TextView
    private lateinit var countriesCount: TextView
    private lateinit var continentsCount: TextView
    private lateinit var yearsCount: TextView

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

        title = findViewById(R.id.tv_history_profile_title)
        filmsCount = findViewById(R.id.tv_history_films_count)
        countriesCount = findViewById(R.id.tv_history_countries_count)
        continentsCount = findViewById(R.id.tv_history_continents_count)
        yearsCount = findViewById(R.id.tv_history_years_count)
        recyclerViewSavedFilms = findViewById(R.id.recycler_view_saved_films)

        savedMovieAdapter = SavedMovieAdapter()

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewSavedFilms.layoutManager = layoutManager
        recyclerViewSavedFilms.adapter = savedMovieAdapter

        lifecycleScope.launch {
            val numberFilms = dao.getFilmsCount()

            if(dao.getTitle() != "null"){
                if(numberFilms == 0) {
                    title.text = getString(R.string.tv_history_profile_title, "Awaiting your next journey")
                } else if (numberFilms <= 10) {
                    title.text = getString(R.string.tv_history_profile_title, dao.getTitle())
                } else if (numberFilms <= 15){
                    title.text = getString(R.string.tv_history_profile_title, "A dream traveler")
                } else if (numberFilms <= 25){
                    title.text = getString(R.string.tv_history_profile_title, "A professor of archaeology")
                } else if (numberFilms <= 35){
                    title.text = getString(R.string.tv_history_profile_title, "A knight of the round table")
                } else if (numberFilms <= 49){
                    title.text = getString(R.string.tv_history_profile_title, "A barefoot wanderer of Middle-Earth")
                } else {
                    title.text = getString(R.string.tv_history_profile_title, "A space Odysseus")
                }
            }

            filmsCount.text = getString(R.string.tv_history_films_count, numberFilms)
            countriesCount.text = getString(R.string.tv_history_countries_count, dao.getCountriesCount())
            continentsCount.text = getString(R.string.tv_history_continents_count, dao.getContinentsCount())
            yearsCount.text = getString(R.string.tv_history_years_count, dao.getYearsCount())

            dao.getSavedMovieForProfile(1).collectLatest { savedMovies ->
                savedMovieAdapter.submitList(savedMovies)
            }
        }

    }

    private fun configureToolbar(toolbar: Toolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.setBackgroundDrawable(getDrawable(R.color.background))
    }

}
