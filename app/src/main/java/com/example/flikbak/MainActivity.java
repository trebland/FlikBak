package com.example.flikbak;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView moviesList;
    private MoviesAdapter adapter;
    private LibraryMoviesAdapter adapterL;

    private MoviesRepository moviesRepository;

    private List<Genre> movieGenres;
    private boolean isFetchingMovies;
    private int currentPage = 1;

    private String sortBy = MoviesRepository.POPULAR;

    DatabaseHelper mDatabaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDatabaseHelper = new DatabaseHelper(this);

        moviesRepository = MoviesRepository.getInstance();

        moviesList = findViewById(R.id.movies_list);
        moviesList.setLayoutManager(new LinearLayoutManager(this));

        setupOnScrollListener();

        getGenres();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_movies, menu);

        // Implements our Library View button
        final Button btnViewData =
                (Button) menu.findItem(R.id.app_bar_library).getActionView();
        btnViewData.setText("Library");
        btnViewData.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view) {

                // Works, but won't look as nice,
                // .. could be utilized if no internet connection is found
                /*
                Intent intent = new Intent(MainActivity.this, ListLibraryActivity.class);
                startActivity(intent);
                */
                currentPage = 1;

                sortBy = MoviesRepository.LIBRARY;
                getLibraryMovies(currentPage, mDatabaseHelper, null);
            }
        });

        // Implements our Search Bar
        SearchView searchView =
                (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                currentPage = 1;

                sortBy = MoviesRepository.SEARCH;
                if (adapterL != null)
                    getLibraryMovies(currentPage, mDatabaseHelper, s);
                else
                    getMovies(currentPage, s);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });

        searchView.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View view) {
                btnViewData.setText("");
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                currentPage = 1;

                if(adapterL != null)
                    getLibraryMovies(currentPage, mDatabaseHelper, null);
                else {
                    sortBy = MoviesRepository.POPULAR;
                    getMovies(currentPage, null);
                }

                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort:
                showSortMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void AddData(Movie movie, String genres, Button button)
    {
        boolean insertData = mDatabaseHelper.AddData(movie, genres);

        if (insertData) {
            button.setText("Added");

            //toastMessage("Data Successfully Inserted!");
        } else {
            //toastMessage("Something went wrong");
        }
    }

    public void DeleteData(Movie movie, Button button) {
        mDatabaseHelper.DeleteData(movie);
        button.setText("Deleted");
    }

    private void showSortMenu() {
        PopupMenu sortMenu = new PopupMenu(this, findViewById(R.id.sort));
        sortMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                /*
                 * Every time we sort, we need to go back to page 1
                 */
                currentPage = 1;

                switch (item.getItemId()) {
                    case R.id.popular:
                        sortBy = MoviesRepository.POPULAR;
                        getMovies(currentPage, null);
                        return true;
                    case R.id.top_rated:
                        sortBy = MoviesRepository.TOP_RATED;
                        getMovies(currentPage, null);
                        return true;
                    case R.id.upcoming:
                        sortBy = MoviesRepository.UPCOMING;
                        getMovies(currentPage, null);
                        return true;
                    default:
                        return false;
                }
            }
        });
        sortMenu.inflate(R.menu.menu_movies_sort);
        sortMenu.show();
    }

    private void setupOnScrollListener() {
        final LinearLayoutManager manager = new LinearLayoutManager(this);
        moviesList.setLayoutManager(manager);
        moviesList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int totalItemCount = manager.getItemCount();
                int visibleItemCount = manager.getChildCount();
                int firstVisibleItem = manager.findFirstVisibleItemPosition();

                if (firstVisibleItem + visibleItemCount >= totalItemCount / 2) {
                    if (!isFetchingMovies && adapterL == null) {
                        getMovies(currentPage + 1, null);
                    }
                }
            }
        });
    }

    private void getGenres() {
        moviesRepository.getGenres(new OnGetGenresCallback() {
            @Override
            public void onSuccess(List<Genre> genres) {
                movieGenres = genres;
                getMovies(currentPage, null);
            }

            @Override
            public void onError() {
                showError();
            }
        });
    }

    private void getMovies(int page, String query) {
        isFetchingMovies = true;
        moviesRepository.getMovies(page, sortBy, query, new OnGetMoviesCallback() {
            @Override
            public void onSuccess(int page, List<Movie> movies) {
                if (adapterL != null)
                    adapterL = null;

                if (adapter == null) {
                    adapter = new MoviesAdapter(movies, movieGenres, callback, callback_add, mDatabaseHelper);
                    moviesList.setAdapter(adapter);
                } else {
                    if (page == 1) {
                        adapter.clearMovies();
                    }
                    adapter.appendMovies(movies);
                }
                currentPage = page;
                isFetchingMovies = false;

                setTitle();
            }

            @Override
            public void onError() {
                showError();
            }
        });
    }

    private void getLibraryMovies(int page, DatabaseHelper database, String query)
    {
        List<Movie> movieList;
        if (sortBy == moviesRepository.SEARCH && query != null)
            movieList = database.GetSearchedMovies(database, query);
        else
            movieList = database.GetMovies(database);

        isFetchingMovies = true;

        if (adapter != null)
            adapter = null;

        if (adapterL == null) {
            adapterL = new LibraryMoviesAdapter(movieList, movieGenres, callback, callback_delete, mDatabaseHelper);
            moviesList.setAdapter(adapterL);
        } else {
            if (page == 1) {
                adapterL.clearMovies();
            }
            adapterL.appendMovies(movieList);
        }
        currentPage = page;
        isFetchingMovies = false;

        setTitle();
    }

    // The callback itself is functional
    OnAddMovieCallback callback_add = new OnAddMovieCallback() {
        @Override
        public void onClick(Movie movie, String genres, Button button) {
            String newEntry = movie.getTitle();
            if (newEntry.length() != 0)
            {
                AddData(movie, genres, button);
            }
            else
                toastMessage("Issue obtaining name");
        }
    };

    OnDeleteMovieCallback callback_delete = new OnDeleteMovieCallback() {
        @Override
        public void onClick(Movie movie, Button button) {
            DeleteData(movie, button);
        }
    };

    OnMoviesClickCallback callback = new OnMoviesClickCallback() {
        @Override
        public void onClick(Movie movie) {
            Intent intent = new Intent(MainActivity.this, MovieActivity.class);
            intent.putExtra(MovieActivity.MOVIE_ID, movie.getId());
            startActivity(intent);
        }
    };



    private void setTitle() {
        switch (sortBy) {
            case MoviesRepository.POPULAR:
                setTitle(getString(R.string.popular));
                break;
            case MoviesRepository.TOP_RATED:
                setTitle(getString(R.string.top_rated));
                break;
            case MoviesRepository.UPCOMING:
                setTitle(getString(R.string.upcoming));
                break;
            case MoviesRepository.LIBRARY:
                setTitle("Library");
                break;
        }
    }

    private void toastMessage (String toToast)
    {
        Toast.makeText(MainActivity.this, toToast, Toast.LENGTH_SHORT).show();
    }

    private void showError() {
        //Toast.makeText(MainActivity.this, "Please check your internet connection.", Toast.LENGTH_SHORT).show();
    }
}
