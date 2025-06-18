package lucaslimb.com.github.cinemap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lucaslimb.com.github.cinemap.data.db.AppDatabase
import lucaslimb.com.github.cinemap.data.models.MovieMarkerInfo
import lucaslimb.com.github.cinemap.data.models.SavedMovie

class MovieDetailsDialogFragment : DialogFragment() {

    private lateinit var dao: lucaslimb.com.github.cinemap.data.dao.ProfileDAO
    private var isMovieSaved: Boolean = false

    companion object {
        private const val ARG_MOVIE_INFO = "movie_info"

        fun newInstance(movieInfo: MovieMarkerInfo): MovieDetailsDialogFragment {
            val fragment = MovieDetailsDialogFragment()
            val args = Bundle().apply {
                putParcelable(ARG_MOVIE_INFO, movieInfo)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_TransparentDialog)
        dao = AppDatabase.getDatabase(requireContext()).profileDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_movie_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val movieInfo = requireArguments().getParcelable<MovieMarkerInfo>(ARG_MOVIE_INFO)
        val btnSave: ImageButton = view.findViewById(R.id.btn_save)
        val frameLayout: FrameLayout = view.findViewById(R.id.btn_save_frame)
        val framePoster: FrameLayout = view.findViewById(R.id.movie_poster_frame)

        if (movieInfo != null) {
            val tvTitle = view.findViewById<TextView>(R.id.tv_title_info_window)
            val tvGenre = view.findViewById<TextView>(R.id.tv_country_info_window)
            val tvDuration = view.findViewById<TextView>(R.id.tv_duration_info_window)
            val tvDirector = view.findViewById<TextView>(R.id.tv_director_info_window)
            val ivPoster = view.findViewById<ImageView>(R.id.info_image)

            tvTitle.text = movieInfo.originalTitle
            tvGenre.text = movieInfo.mainGenre ?: resources.getString(R.string.na)
            tvDuration.text =
                "${movieInfo.duration?.toString() ?: resources.getString(R.string.na)} min"
            tvDirector.text = movieInfo.director

            if (!movieInfo.posterUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(movieInfo.posterUrl)
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(ivPoster)
            } else {
                ivPoster.setImageResource(R.drawable.placeholder_poster)
            }

            lifecycleScope.launch {
                isMovieSaved = dao.isMovieSaved(movieInfo.movieId, 1)
                if (isMovieSaved) {
                    frameLayout.visibility = View.INVISIBLE
                } else {
                    frameLayout.visibility = View.VISIBLE
                }
            }
        }

        btnSave.setOnClickListener {
            lifecycleScope.launch {
                save(movieInfo!!)
            }

            frameLayout.post {
                framePoster.post {
                    val slideDownDistance = frameLayout.height + framePoster.height

                    val slideDownAnimation = TranslateAnimation(
                        0f, 0f,
                        0f, slideDownDistance.toFloat()
                    )
                    slideDownAnimation.duration = 400
                    slideDownAnimation.fillAfter = true

                    slideDownAnimation.setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {
                        }

                        override fun onAnimationEnd(animation: Animation?) {
                            frameLayout.visibility = View.INVISIBLE
                        }

                        override fun onAnimationRepeat(animation: Animation?) {
                        }
                    })

                    frameLayout.startAnimation(slideDownAnimation)
                }
            }
        }

    }

    private suspend fun save(movieInfo: MovieMarkerInfo){
        val newSavedMovie = SavedMovie(
            profileId = 1,
            movieId = movieInfo.movieId,
            title = movieInfo.originalTitle,
            posterUrl = movieInfo.posterUrl,
            director = movieInfo.director,
            country = movieInfo.country,
            releaseYear = movieInfo.releaseYear
        )
        val rowId = dao.saveMovie(newSavedMovie)
    }

}