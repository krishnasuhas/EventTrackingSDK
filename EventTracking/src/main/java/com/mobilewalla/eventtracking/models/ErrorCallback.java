package com.mobilewalla.eventtracking.models;

public interface ErrorCallback<T> {
    void onError(T errorMessage);
}
