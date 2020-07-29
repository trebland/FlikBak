package com.example.flikbak;

public interface OnGetMovieCallback {

    void onSuccess(Movie movie);

    void onError();
}