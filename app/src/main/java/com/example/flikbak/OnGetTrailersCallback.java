package com.example.flikbak;

import java.util.List;

public interface OnGetTrailersCallback {
    void onSuccess(List<Trailer> trailers);

    void onError();
}