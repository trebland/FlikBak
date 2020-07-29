package com.example.flikbak;

import android.widget.Button;

/**
 * Created by 18103538X on 4/12/2019.
 */

public interface OnAddMovieCallback {
    void onClick (Movie movie, String genres, Button button);
}
