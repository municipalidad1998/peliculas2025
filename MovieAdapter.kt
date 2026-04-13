package com.streamflix.reborn.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamflix.reborn.data.model.Movie
import com.streamflix.reborn.databinding.ItemMovieBinding // Asumiendo que usas ViewBinding

class MovieAdapter(private val movies: List<Movie>, private val onClick: (Movie) -> Unit) :
    RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.binding.titleTextView.text = movie.title
        Glide.with(holder.itemView.context).load(movie.imageUrl).into(holder.binding.posterImageView)
        holder.itemView.setOnClickListener { onClick(movie) }
    }

    override fun getItemCount() = movies.size
}
