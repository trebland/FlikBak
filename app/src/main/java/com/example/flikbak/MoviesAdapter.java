package com.example.flikbak;

import android.content.Context;
import android.provider.ContactsContract;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MovieViewHolder> {
    private DatabaseHelper database;

    private String IMAGE_BASE_URL = "http://image.tmdb.org/t/p/w500";
    private List<Genre> allGenres;
    private List<Movie> movies;
    private OnMoviesClickCallback callback;
    private OnAddMovieCallback callback_add;

    public MoviesAdapter(List<Movie> movies, List<Genre> allGenres, OnMoviesClickCallback callback, OnAddMovieCallback callback_add, DatabaseHelper database) {
        this.callback = callback;
        this.callback_add = callback_add;
        this.movies = movies;
        this.allGenres = allGenres;
        this.database = database;
    }

    @Override
    public MovieViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MovieViewHolder holder, int position) {
        holder.bind(movies.get(position));
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        TextView releaseDate;
        TextView title;
        TextView rating;
        TextView genres;
        ImageView poster;
        Movie movie;
        Button addMovie;

        public MovieViewHolder(View itemView) {
            super(itemView);
            releaseDate = itemView.findViewById(R.id.item_movie_release_date);
            title = itemView.findViewById(R.id.item_movie_title);
            rating = itemView.findViewById(R.id.item_movie_rating);
            genres = itemView.findViewById(R.id.item_movie_genre);
            poster = itemView.findViewById(R.id.item_movie_poster);

            // Enables the addition of movies
            addMovie = itemView.findViewById(R.id.item_button_add);
            addMovie.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v) {
                    callback_add.onClick(movie, genres.getText().toString(), addMovie);
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    callback.onClick(movie);
                }
            });
        }


        public void bind(Movie movie) {
            this.movie = movie;
            releaseDate.setText(movie.getReleaseDate().split("-")[0]);
            title.setText(movie.getTitle());
            rating.setText(String.valueOf(movie.getRating()));
            genres.setText(getGenres(movie.getGenreIds()));
            Glide.with(itemView).load(IMAGE_BASE_URL + movie.getPosterPath()).apply(RequestOptions.placeholderOf(R.color.colorPrimary)).into(poster);
            addMovie.setText("Add");


            if (database.ExistsInDatabase(movie.getTitle(), movie.getReleaseDate()))
            {
                addMovie.setText("In Library");
            }
            else
                addMovie.setText("Add");

        }

        private String getGenres(List<Integer> genreIds) {
            List<String> movieGenres = new ArrayList<>();
            for (Integer genreId : genreIds) {
                for (Genre genre : allGenres) {
                    if (genre.getId() == genreId) {
                        movieGenres.add(genre.getName());
                        break;
                    }
                }
            }
            return TextUtils.join(", ", movieGenres);
        }
    }


    public void appendMovies(List<Movie> moviesToAppend) {
        movies.addAll(moviesToAppend);
        notifyDataSetChanged();
    }
    public void clearMovies() {
        movies.clear();
        notifyDataSetChanged();
    }
}