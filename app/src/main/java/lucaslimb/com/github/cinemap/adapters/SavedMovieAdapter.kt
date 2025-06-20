package lucaslimb.com.github.cinemap.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import lucaslimb.com.github.cinemap.R
import lucaslimb.com.github.cinemap.data.dao.ProfileDAO
import lucaslimb.com.github.cinemap.data.models.SavedMovie

class SavedMovieAdapter(private val dao: ProfileDAO,
                        private val lifecycleScope: LifecycleCoroutineScope) :
                        ListAdapter<SavedMovie, SavedMovieAdapter.SavedMovieViewHolder>(SavedMovieDiffCallback()) {

    var onMovieDeletedCallback: (() -> Unit)? = null

    fun getMovieAtPosition(position: Int): SavedMovie {
        return getItem(position)
    }

    inner class SavedMovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posterImageView: ImageView = itemView.findViewById(R.id.item_info_image)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btn_remove)
        val frameRemove: FrameLayout = itemView.findViewById(R.id.btn_remove_frame)

        val cornerRadiusPx = (itemView.context.resources.displayMetrics.density * 8f).toInt()
        fun bind(movie: SavedMovie) {
            if (!movie.posterUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(movie.posterUrl)
                    .apply(RequestOptions.bitmapTransform(RoundedCorners(cornerRadiusPx)))
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(posterImageView)
            } else {
                posterImageView.setImageResource(R.drawable.placeholder_poster)
            }

            itemView.setOnClickListener {
                if (frameRemove.visibility == View.INVISIBLE) {
                    frameRemove.post {
                        val startY = frameRemove.height.toFloat()

                        val slideDownAnimation = TranslateAnimation(
                            0f, 0f,
                            startY, 0f
                        )
                        slideDownAnimation.duration = 400
                        slideDownAnimation.fillAfter = true

                        slideDownAnimation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {
                                frameRemove.visibility = View.VISIBLE
                            }

                            override fun onAnimationEnd(animation: Animation?) {
                            }

                            override fun onAnimationRepeat(animation: Animation?) {}
                        })

                        frameRemove.startAnimation(slideDownAnimation)
                    }
                } else if(frameRemove.visibility == View.VISIBLE) {
                    frameRemove.post {
                        val endY = frameRemove.height.toFloat()

                        val slideUpAnimation = TranslateAnimation(
                            0f, 0f,
                            0f, endY
                        )
                        slideUpAnimation.duration = 400
                        slideUpAnimation.fillAfter = true

                        slideUpAnimation.setAnimationListener(object : Animation.AnimationListener {
                            override fun onAnimationStart(animation: Animation?) {}
                            override fun onAnimationEnd(animation: Animation?) {
                                frameRemove.visibility = View.INVISIBLE
                            }
                            override fun onAnimationRepeat(animation: Animation?) {}
                        })

                        frameRemove.startAnimation(slideUpAnimation)
                    }
                }
            }
            btnRemove.setOnClickListener {
                lifecycleScope.launch {
                    dao.deleteMovie(movie)
                    onMovieDeletedCallback?.invoke()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedMovieViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recycler_saved_films, parent, false)
        return SavedMovieViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedMovieViewHolder, position: Int) {
        val movie = getItem(position)
        holder.bind(movie)
    }

    private class SavedMovieDiffCallback : DiffUtil.ItemCallback<SavedMovie>() {
        override fun areItemsTheSame(oldItem: SavedMovie, newItem: SavedMovie): Boolean {
            return oldItem.movieId == newItem.movieId
        }

        override fun areContentsTheSame(oldItem: SavedMovie, newItem: SavedMovie): Boolean {
            return oldItem == newItem
        }
    }

}