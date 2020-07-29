package com.example.flikbak;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * Created by 18103538X on 4/11/2019.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String TABLE_NAME = "movie_table";
    private static final String COL1 = "ID";
    private static final String COL2 = "title";
    private static final String COL3 = "release";
    private static final String COL4 = "rating";
    private static final String COL5 = "genre_names";
    private static final String COL6 = "genre_ids";
    private static final String COL7 = "poster";
    private static final String COL8 = "movie_id";
    private static final String COL9 = "overview";
    private static final String COL10 = "backdrop";


    public DatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 12);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL2 + " TEXT, " + COL3 + " TEXT, " + COL4 + " REAL, " + COL5 + " TEXT, " + COL6 + " TEXT, " +
                COL7 + " TEXT, " + COL8 + " TEXT, " + COL9 + " TEXT, " + COL10 + " TEXT" + ")";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean AddData(Movie movie, String genres) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String title = movie.getTitle();

        if (title.contains("'"))
            title = RemoveApostrophes(title);

        // If our database already holds the movie,
        // .. then do not re-add (check release date and title)
        // .. in case of reboots
        if (ExistsInDatabase(title, movie.getReleaseDate()))
            return false;

        contentValues.put(COL2, movie.getTitle());
        contentValues.put(COL3, movie.getReleaseDate());
        contentValues.put(COL4, movie.getRating());

        // We will re-iterate through the genre strings to recreate the
        // .. genre objects (this will combine with information from COL6
        contentValues.put(COL5, genres);

        StringBuilder strbul  = new StringBuilder();
        Iterator<Integer> iter = movie.getGenreIds().iterator();
        while(iter.hasNext())
        {
            strbul.append(iter.next());
            if(iter.hasNext()){
                strbul.append(" ");
            }
        }

        contentValues.put(COL6, strbul.toString());
        contentValues.put(COL7, movie.getPosterPath());
        contentValues.put(COL8, movie.getId());
        contentValues.put(COL9, movie.getOverview());
        contentValues.put(COL10,movie.getBackdrop());

        //Log.d(TAG, "addData: Adding " + item + " to " + TABLE_NAME);

        long result = db.insert(TABLE_NAME, null, contentValues);

        // Upon incorrect insertion, it will return -1
        if (result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public String RemoveApostrophes (String ErroneousName)
    {
        return ErroneousName.replace("'", "''");
    }

    // As of right now, it will crash if the title contains an apostrophe
    // .. this is due to SQL limitations
    // .. (which would require a little more work and I don't feel like it)
    public boolean ExistsInDatabase(String title, String release)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        if (title.contains("'"))
            title = RemoveApostrophes(title);

        String query = "SELECT " + COL1 + " FROM " + TABLE_NAME +
            " WHERE " + COL2 + " = '" + title + "'"
                + " AND "
                      + COL3 + " = '" + release + "'";
        Cursor cursor = db.rawQuery(query, null);
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    public List<Movie> GetSearchedMovies (DatabaseHelper database, String query)
    {
        List<Movie> movies = new ArrayList<>();
        Cursor cursor = database.getSearchedData(query);

        if (cursor.getCount() > 0)
        {
            while(cursor.moveToNext())
                movies.add(GetMovie(cursor));
        }

        cursor.close();
        return movies;

    }

    public List<Movie> GetMovies (DatabaseHelper database)
    {
        List<Movie> movies = new ArrayList<>();
        Cursor cursor = database.getData();

        if (cursor.getCount() > 0)
        {
            while(cursor.moveToNext())
                movies.add(GetMovie(cursor));
        }

        cursor.close();
        return movies;
    }

    public Movie GetMovie(Cursor cursor)
    {
        //if(!ExistsInDatabase(title, release))
            //return null;
        Movie movie = new Movie();

        movie.setTitle(cursor.getString(1));
        movie.setReleaseDate(cursor.getString(2));

        // Converts our rating to a float
        movie.setRating(Float.valueOf(cursor.getString(3)));

        // Converts our id's and name strings into genres while also satisfying the
        // .. previous requirement of each movie object holding a list of Genre's and
        // .. a list of said genre's ids
        Scanner nameScanner = new Scanner(cursor.getString(4));
        Scanner idScanner = new Scanner(cursor.getString(5));

        List<Genre> genres = new ArrayList<>();
        Genre tempGenre = new Genre();
        List<Integer> integerList = new ArrayList<>();
        Integer tempID;
        while (nameScanner.hasNext() && idScanner.hasNextInt()) {
            tempGenre.setName(nameScanner.next());
            tempID = idScanner.nextInt();
            tempGenre.setId(tempID);

            genres.add(tempGenre);
            integerList.add(tempID);
        }

        movie.setGenres(genres);
        movie.setGenreIds(integerList);

        // Close our scanners as we no longer need them
        nameScanner.close();
        idScanner.close();

        movie.setPosterPath(cursor.getString(6));

        // Converts our id to an integer
        movie.setId(Integer.valueOf(cursor.getString(7)));
        movie.setOverview(cursor.getString(8));
        movie.setBackdrop(cursor.getString(9));

        return movie;
    }


    /**
     * Returns all the data from database
     * @return
     */
    public Cursor getData(){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        Cursor data = db.rawQuery(query, null);
        return data;
    }

    /**
     * Returns only the ID that matches the name passed in
     * @param title
     * @return
     */
    // Currently getSearchedData isn't working as intended
    // .. however, all other calls are functioning well
    public Cursor getSearchedData(String title){
        SQLiteDatabase db = this.getWritableDatabase();
        if (title.contains("'"))
            title = RemoveApostrophes(title);

        String query = "SELECT * FROM " + TABLE_NAME +
                " WHERE " + COL2 + " LIKE '%" + title + "%'";
        Cursor cursor = db.rawQuery(query, null);
        return cursor;
    }

    /**
     * Updates the name field
     * @param newName
     * @param id
     * @param oldName
     */
    public void updateName(String newName, int id, String oldName){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_NAME + " SET " + COL2 +
                " = '" + newName + "' WHERE " + COL1 + " = '" + id + "'" +
                " AND " + COL2 + " = '" + oldName + "'";
        Log.d(TAG, "updateName: query: " + query);
        Log.d(TAG, "updateName: Setting name to " + newName);
        db.execSQL(query);
    }

    /**
     * Delete from database
     * @param movie
     */
    public void DeleteData(Movie movie){
        SQLiteDatabase db = this.getWritableDatabase();
        String title = movie.getTitle();
        String release = movie.getReleaseDate();

        String query = "DELETE FROM " + TABLE_NAME + " WHERE "
                + COL2 + " = '" + title + "'" +
                " AND " + COL3 + " = '" + release + "'";
        Log.d(TAG, "deleteName: query: " + query);
        Log.d(TAG, "deleteName: Deleting " + title + " from database.");
        db.execSQL(query);
    }
}
