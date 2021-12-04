package com.mobilewalla.eventtracking.models;

public interface ResponseCallback<T> {
    void onResponse(T response);
}