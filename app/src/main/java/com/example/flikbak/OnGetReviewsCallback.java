package com.example.flikbak;

import java.util.List;

public interface OnGetReviewsCallback {
    void onSuccess(List<Review> reviews);

    void onError();
}