package lucaslimb.com.github.cinemap.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import lucaslimb.com.github.cinemap.R
import lucaslimb.com.github.cinemap.data.models.SavedMovie

class SavedMovieAdapter : ListAdapter<SavedMovie, SavedMovieAdapter.SavedMovieViewHolder>(SavedMovieDiffCallback()) {
    inner class SavedMovieViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posterImageView: ImageView = itemView.findViewById(R.id.item_info_image)

        fun bind(movie: SavedMovie) {
            if (!movie.posterUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(movie.posterUrl)
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(posterImageView)
            } else {
                posterImageView.setImageResource(R.drawable.placeholder_poster)
            }
            itemView.setOnClickListener {
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