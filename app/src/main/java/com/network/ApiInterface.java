package com.network;

import com.example.models.ImageModel;
import com.example.models.SegmentResponseModel;

import org.json.JSONObject;

import io.reactivex.Observable;
import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiInterface {

    @Headers({"Content-Type:application/json",
            "x-api-key:INSECURE-6"})
    @POST("segment")
    Observable<SegmentResponseModel> uploadImage(@Body ImageModel img);
}
