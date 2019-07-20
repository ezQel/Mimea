package com.ezqel.mimea;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadAPIs {

    @Multipart
    @POST("/uploader")
    Call<JsonObject> uploadImage(@Part MultipartBody.Part file, @Part("file") RequestBody requestBody);
}
